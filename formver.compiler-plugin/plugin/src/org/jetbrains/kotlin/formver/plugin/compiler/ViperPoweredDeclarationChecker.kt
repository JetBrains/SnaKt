/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.formver.common.*
import org.jetbrains.kotlin.formver.core.conversion.ProgramConverter
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.print
import org.jetbrains.kotlin.formver.plugin.compiler.reporting.reportVerifierError
import org.jetbrains.kotlin.formver.viper.Verifier
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.formver.viper.ast.registerAllNames
import org.jetbrains.kotlin.formver.viper.ast.unwrapOr
import org.jetbrains.kotlin.formver.viper.errors.VerifierError
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// A function "has a contract" if its FIR contract description is resolved and contains at least one effect.
// Used by TargetsSelection.TARGETS_WITH_CONTRACT to decide whether to convert/verify a function.
private val FirContractDescriptionOwner.hasContract: Boolean
    get() = when (val description = contractDescription) {
        is FirResolvedContractDescription -> description.effects.isNotEmpty()
        else -> false
    }

// Maps a TargetsSelection policy to a yes/no decision for a specific function declaration.
// ALL_TARGETS: always convert; TARGETS_WITH_CONTRACT: only if the function has Kotlin contracts;
// NO_TARGETS: never convert (plugin effectively disabled).
private fun TargetsSelection.applicable(declaration: FirSimpleFunction): Boolean = when (this) {
    TargetsSelection.ALL_TARGETS -> true
    TargetsSelection.TARGETS_WITH_CONTRACT -> declaration.hasContract
    TargetsSelection.NO_TARGETS -> false
}

// The main FIR checker. The Kotlin compiler invokes `check()` once per FirSimpleFunction
// encountered during analysis. This class drives the entire conversion → verification pipeline.
class ViperPoweredDeclarationChecker(
    private val session: FirSession,
    private val config: PluginConfiguration,
    private val viperDumpFileManager: ViperDumpFileManager?,
) : FirSimpleFunctionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirSimpleFunction) {
        // Step 1: Gate — skip functions that are excluded by plugin configuration or annotation.
        if (!config.shouldConvert(declaration)) return

        val errorCollector = ErrorCollector()
        try {
            // Step 2: Convert — translate the FIR function into a Viper program embedding.
            // ProgramConverter walks the FIR AST and builds the intermediate Viper representation.
            // `registerForVerification` is the entry point: it embeds the function body,
            // checks purity (for @Pure functions), and registers pre/postconditions.
            val programConversionContext = ProgramConverter(session, config, errorCollector)
            programConversionContext.registerForVerification(declaration)
            val program = programConversionContext.program

            // Step 3: Name resolution — register all symbolic names in the program
            // so that debug output and error messages can refer to human-readable identifiers.
            with(programConversionContext.nameResolver) {
                program.registerAllNames()
            }

            // Step 4 (optional): Logging — if the log level is set above ONLY_WARNINGS,
            // emit the Viper program text as a VIPER_TEXT diagnostic on the function source.
            getProgramForLogging(program)?.let {
                reporter.reportOn(
                    declaration.source,
                    PluginErrors.VIPER_TEXT,
                    declaration.name.asString(),
                    with(programConversionContext.nameResolver) { it.toDebugOutput() },
                )
            }

            // Step 4b (optional): File dump — if a ViperDumpFileManager was provided, write the
            // full Viper program to .formver/<name>.vpr and emit the file URI as a VIPER_FILE diagnostic.
            viperDumpFileManager?.let { manager ->
                val declarationName = declaration.name.asString()
                val dumpText = with(programConversionContext.nameResolver) { program.toDebugOutput() }
                val fileUri = manager.writeViperDump(declarationName, dumpText)
                reporter.reportOn(
                    declaration.source,
                    PluginErrors.VIPER_FILE,
                    declarationName,
                    fileUri.toString(),
                )
            }

            // Step 5 (optional): Expression embedding dump — if @DumpExpEmbeddings is present,
            // emit each intermediate expression embedding as a diagnostic for inspection.
            if (shouldDumpExpEmbeddings(declaration)) {
                with(programConversionContext.nameResolver) {
                    for ((name, embedding) in programConversionContext.debugExpEmbeddings) {
                        reporter.reportOn(
                            declaration.source,
                            PluginErrors.EXP_EMBEDDING,
                            name.mangled,
                            embedding.debugTreeView.print()
                        )
                    }
                }
            }

            // Step 6: Purity errors — report any violations collected during conversion
            // (e.g. a @Pure function body contained a MethodCall or field access).
            errorCollector.forEachPurityError { source, errorMessage ->
                reporter.reportOn(source, PluginErrors.PURITY_VIOLATION, errorMessage)
            }

            // Step 7: Consistency check — verify that the generated Viper program is
            // internally well-formed (type-correct, etc.) before running Silicon.
            // If it is not, that is a plugin bug, not a user error; skip further verification.
            val verifier = Verifier()
            val onFailure = { err: VerifierError ->
                val source = err.position.unwrapOr { declaration.source }
                reporter.reportVerifierError(source, err, config.errorStyle)
            }
            val viperProgram = with(programConversionContext.nameResolver) { program.toSilver() }
            val consistent = verifier.checkConsistency(viperProgram, onFailure)
            // If the Viper program is not consistent, that's our error; we shouldn't surface it to the user as an unverified contract.
            if (!consistent || !config.shouldVerify(declaration)) return

            // Step 8: Verification — run Silicon (the SMT-backed Viper verifier) on the program.
            // Any failing assertions or violated postconditions are reported via `onFailure`.
            verifier.verify(viperProgram, onFailure)
        } catch (e: SnaktInternalException) {
            // Known internal error with a source location — report precisely.
            reporter.reportOn(e.source, PluginErrors.INTERNAL_ERROR, e.message)
        } catch (e: Exception) {
            // Unknown exception — fall back to reporting on the function declaration itself.
            val error = e.message ?: "No message provided"
            reporter.reportOn(declaration.source, PluginErrors.INTERNAL_ERROR, error)
        }

        // Step 9: Minor errors — non-fatal issues collected during conversion that do not
        // prevent verification but should still be surfaced to the user.
        errorCollector.forEachMinorError {
            reporter.reportOn(declaration.source, PluginErrors.MINOR_INTERNAL_ERROR, it)
        }
    }

    // Returns the program to log based on the configured verbosity level,
    // or null if logging is suppressed (ONLY_WARNINGS).
    private fun getProgramForLogging(program: Program): Program? = when (config.logLevel) {
        LogLevel.ONLY_WARNINGS -> null
        LogLevel.SHORT_VIPER_DUMP -> program.toShort().withoutPredicates()
        LogLevel.SHORT_VIPER_DUMP_WITH_PREDICATES -> program.toShort()
        LogLevel.FULL_VIPER_DUMP -> program
    }

    // Constructs the ClassId for a formver plugin annotation by its simple name.
    private fun getAnnotationId(name: String): ClassId =
        ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")), Name.identifier(name))

    // Pre-computed annotation ClassIds used for fast annotation lookup on FIR symbols.
    private val neverConvertId: ClassId = getAnnotationId("NeverConvert")
    private val neverVerifyId: ClassId = getAnnotationId("NeverVerify")
    private val alwaysVerifyId: ClassId = getAnnotationId("AlwaysVerify")
    private val dumpExpEmbeddingsId: ClassId = getAnnotationId("DumpExpEmbeddings")

    // Decides whether a function should be converted to Viper at all.
    // @NeverConvert overrides all other settings.
    private fun PluginConfiguration.shouldConvert(declaration: FirSimpleFunction): Boolean = when {
        declaration.hasAnnotation(neverConvertId, session) -> false
        else -> conversionSelection.applicable(declaration)
    }

    // Decides whether Silicon should verify the converted Viper program.
    // @NeverConvert and @NeverVerify suppress verification; @AlwaysVerify forces it
    // regardless of the global verificationSelection policy.
    private fun PluginConfiguration.shouldVerify(declaration: FirSimpleFunction): Boolean = when {
        declaration.hasAnnotation(neverConvertId, session) -> false
        declaration.hasAnnotation(neverVerifyId, session) -> false
        declaration.hasAnnotation(alwaysVerifyId, session) -> true
        else -> verificationSelection.applicable(declaration)
    }

    // Returns true if the @DumpExpEmbeddings annotation is present,
    // which triggers emission of intermediate expression embeddings as diagnostics.
    private fun shouldDumpExpEmbeddings(declaration: FirSimpleFunction): Boolean =
        declaration.hasAnnotation(dumpExpEmbeddingsId, session)
}
