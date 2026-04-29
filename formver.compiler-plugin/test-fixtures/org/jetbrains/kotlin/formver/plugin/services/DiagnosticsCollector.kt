package org.jetbrains.kotlin.formver.plugin.services

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithoutSource
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

const val CONVERSION_FILE_EXTENSION = ".fir.diag.txt"
const val VERIFICATION_FILE_EXTENSION = ".viper.diag.txt"


val TestServices.diagnosticsCollector: DiagnosticsCollector by TestServices.testServiceAccessor()

/**
 * TestService to collect diagnostics from the conversion and verification processes.
 */
class DiagnosticsCollector(val testServices: TestServices) : TestService {
    private val conversionDiagnostics: MutableList<KtDiagnostic> = mutableListOf()
    private val verificationDiagnostics: MutableList<KtDiagnostic> = mutableListOf()

    private fun render(diagnostics: List<KtDiagnostic>): String? {
        if (diagnostics.isEmpty()) return null
        val fileName = testServices.moduleStructure.originalTestDataFiles.single().name

        class DiagnosticData(val textRanges: List<TextRange>, val severity: String, val message: String)

        val reportedDiagnostics = diagnostics
            .map {
                DiagnosticData(
                    textRanges = when (it) {
                        is KtDiagnosticWithSource -> it.textRanges
                        is KtDiagnosticWithoutSource -> listOf(it.firstRange)
                    },
                    severity = AnalyzerWithCompilerReport.convertSeverity(it.severity).toString()
                        .toLowerCaseAsciiOnly(),
                    message = it.renderMessage()
                )
            }
            .sortedWith(compareBy<DiagnosticData> { it.textRanges.first().startOffset }.thenBy { it.message })
        return reportedDiagnostics.joinToString(separator = "\n\n") {
            "/$fileName:${it.textRanges.first()}: ${it.severity}: ${it.message}"
        }.trimTrailingWhitespacesAndAddNewlineAtEOF()
    }

    fun renderConversionDiagnostics(): String? = render(conversionDiagnostics)
    fun renderVerificationDiagnostics(): String? = render(verificationDiagnostics)


    fun addConversionDiagnostics(info: FirOutputArtifact) {
        val frontendDiagnosticsPerFile =
            FirDiagnosticCollectorService(testServices).getFrontendDiagnosticsForModule(info)

        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            for (file in currentModule.files) {
                val firFile = info.mainFirFiles[file] ?: continue
                val diagnostics = frontendDiagnosticsPerFile[firFile]
                conversionDiagnostics.addAll(diagnostics.map { it.diagnostic })
            }
        }
    }

    fun addVerificationDiagnostics(info: List<KtDiagnostic>) {
        verificationDiagnostics.addAll(info)
    }

    /**
     * Checks if the conversion has changed compared to the expected output. Returns true if the conversion has changed.
     * Does not perform any assertions.
     */
    fun conversionHasChanged(): Boolean {
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile =
            testDataFile.parentFile.resolve("${testDataFile.nameWithoutExtension.removeSuffix(".fir")}$CONVERSION_FILE_EXTENSION")

        val actualDiagnostics = renderConversionDiagnostics()

        // the golden file does not exist and diagnostics are empty
        if (!expectedFile.exists() && actualDiagnostics == null) {
            return false
        }

        val expectedDiagnostics = expectedFile.readText()

        return expectedDiagnostics.trimTrailingWhitespacesAndAddNewlineAtEOF() != actualDiagnostics?.trimTrailingWhitespacesAndAddNewlineAtEOF()
    }

    /**
     * Asserts equality of the conversion diagnostics with the expected output.
     */
    fun assertConversion() {
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile =
            testDataFile.parentFile.resolve("${testDataFile.nameWithoutExtension.removeSuffix(".fir")}$CONVERSION_FILE_EXTENSION")

        val expectedOutput = renderConversionDiagnostics()
        if (expectedOutput == null && !expectedFile.exists()) {
            return
        }

        testServices.assertions.assertEqualsToFile(expectedFile, expectedOutput ?: "")
    }

    /**
     * Asserts equality of the verification diagnostics with the expected output.
     */
    fun assertVerification() {
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile =
            testDataFile.parentFile.resolve("${testDataFile.nameWithoutExtension.removeSuffix(".fir")}$VERIFICATION_FILE_EXTENSION")

        val expectedOutput = renderVerificationDiagnostics()
        if (expectedOutput == null && !expectedFile.exists()) {
            return
        }

        testServices.assertions.assertEqualsToFile(expectedFile, expectedOutput ?: "")

    }
}

