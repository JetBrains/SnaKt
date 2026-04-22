package org.jetbrains.kotlin.formver.plugin.services

import org.jetbrains.kotlin.diagnostics.InternalDiagnosticFactoryMethod
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.formver.core.shouldVerify
import org.jetbrains.kotlin.formver.core.viperProgram
import org.jetbrains.kotlin.formver.plugin.compiler.VerificationErrors
import org.jetbrains.kotlin.formver.plugin.compiler.reporting.*
import org.jetbrains.kotlin.formver.plugin.runners.TestMode
import org.jetbrains.kotlin.formver.plugin.runners.getTestMode
import org.jetbrains.kotlin.formver.plugin.runners.runChecks
import org.jetbrains.kotlin.formver.viper.SiliconFrontend
import org.jetbrains.kotlin.formver.viper.ast.unwrapOr
import org.jetbrains.kotlin.formver.viper.errors.ConsistencyError
import org.jetbrains.kotlin.formver.viper.errors.VerificationError
import org.jetbrains.kotlin.formver.viper.errors.VerifierError
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices

fun shouldSkipByTestMode(testServices: TestServices): Boolean = when (getTestMode()) {
    TestMode.CHECK_CONVERSION -> true
    TestMode.UPDATE -> !testServices.diagnosticsCollector.conversionHasChanged()
    TestMode.FULL -> false
}


class ViperProgramVerificationFacade(val testServices: TestServices) :
    AbstractTestFacade<FirOutputArtifact, FirOutputArtifact>() {
    override val inputKind: TestArtifactKind<FirOutputArtifact>
        get() = FrontendKinds.FIR
    override val outputKind: TestArtifactKind<FirOutputArtifact>
        get() = FrontendKinds.FIR

    /**
     * Returns true iff the given [decl] should be verified.
     */
    fun shouldVerify(decl: FirSimpleFunction, testServices: TestServices): Boolean =
        decl.shouldVerify == true && !shouldSkipByTestMode(testServices)

    /**
     * Extracts the Viper programs and if necessary, verifies them.
     * The result is registered in the [testServices].
     */
    @OptIn(TestInfrastructureInternals::class)
    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): FirOutputArtifact {
        inputArtifact.partsForDependsOnModules.forEach { part ->
            val currentModule = part.module
            val toVerify: MutableList<Pair<TestFile, FirSimpleFunction>> = mutableListOf()
            part.firFiles.map { (testFile, firFile) ->
                firFile.accept(object : FirDefaultVisitorVoid() {
                    override fun visitElement(element: FirElement) {
                        when (element) {
                            is FirSimpleFunction -> visitSimpleFunction(element)
                            else -> element.acceptChildren(this)
                        }
                    }

                    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
                        if (shouldVerify(simpleFunction, testServices)) {
                            toVerify.add(Pair(testFile, simpleFunction))
                        }
                    }
                })
            }
            if (toVerify.isNotEmpty()) {
                val verifier = SiliconFrontend(emptyList())
                with(verifier) {
                    toVerify.forEach { (testFile, decl) ->
                        val diagnostics = verifyFunction(decl, module)
                        testServices.diagnosticsCollector.addVerificationDiagnostics(diagnostics)
                        testServices.tagCollector.reportDiagnostics(testFile, diagnostics)
                    }
                }
//                verifier.close()
            }
        }

        return inputArtifact
    }

    /**
     * Runs the verifier
     */
    @OptIn(InternalDiagnosticFactoryMethod::class)
    context(verifier: SiliconFrontend)
    private fun verifyFunction(
        decl: FirSimpleFunction,
        module: TestModule
    ): List<KtDiagnostic> {
        val results = mutableListOf<KtDiagnostic>()
        val program = decl.viperProgram!!
        val onFailure = { err: VerifierError ->
            when (err) {
                is ConsistencyError -> {}
                is VerificationError -> {
                    val diagnostics = formatVerificationError(err, decl, module)
                    val unsued = results.add(diagnostics)
                }
            }
        }
        verifier.use {
            it.verify(program, onFailure)
        }
        return results
    }

    override fun shouldTransform(module: TestModule): Boolean = true


    @OptIn(InternalDiagnosticFactoryMethod::class)
    private fun formatVerificationError(
        err: VerificationError, decl: FirSimpleFunction,
        module: TestModule
    ): KtDiagnostic {
        val source = err.position.unwrapOr { decl.source }!!
        val diagnostic = when (val formattedError = err.formatUserFriendly()) {
            is ConditionalEffectError -> {
                val msg = formattedError.msg()
                VerificationErrors.CONDITIONAL_EFFECT_ERROR.on(
                    source, msg.first, msg.second,
                    positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
                    languageVersionSettings = module.languageVersionSettings
                )
            }

            is DefaultError -> {
                val msg = formattedError.msg()
                VerificationErrors.VIPER_VERIFICATION_ERROR.on(
                    source, msg,
                    positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
                    languageVersionSettings = module.languageVersionSettings
                )
            }

            is IndexOutOfBoundError -> {
                val msg = formattedError.msg()
                VerificationErrors.POSSIBLE_INDEX_OUT_OF_BOUND.on(
                    source, msg.first, msg.second,
                    positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
                    languageVersionSettings = module.languageVersionSettings
                )
            }

            is InvalidSubListRangeError -> {
                val msg = formattedError.msg()
                VerificationErrors.INVALID_SUBLIST_RANGE.on(
                    source, msg.first, msg.second,
                    positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
                    languageVersionSettings = module.languageVersionSettings
                )
            }

            is ReturnsEffectError -> {
                val msg = formattedError.msg()
                VerificationErrors.UNEXPECTED_RETURNED_VALUE.on(
                    source, msg,
                    positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
                    languageVersionSettings = module.languageVersionSettings
                )
            }

            null -> {
                VerificationErrors.VIPER_VERIFICATION_ERROR.on(
                    source, err.msg, positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
                    languageVersionSettings = module.languageVersionSettings
                )
            }
        }
        return diagnostic!!
    }
}


/**
 * If necessary, asserts the verification result as well as the conversion result.
 */
class ViperResultHandler(testServices: TestServices) :
    AnalysisHandler<FirOutputArtifact>(testServices, false, false) {
    override val artifactKind: TestArtifactKind<FirOutputArtifact> get() = FrontendKinds.FIR

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        if (shouldSkipByTestMode(testServices)) return

        runChecks(
            testServices,
            { testServices.diagnosticsCollector.assertVerification() },
            { testServices.diagnosticsCollector.assertConversion() },
            { testServices.tagCollector.assertAll() },
        )
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}