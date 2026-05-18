package org.jetbrains.kotlin.formver.common.services

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
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

/**
 * TestService to collect diagnostics from the conversion and verification processes.
 */
abstract class DiagnosticsCollector(val testServices: TestServices) : TestService {
    abstract val fileExtension: String

    private val diagnostics: MutableList<KtDiagnostic> = mutableListOf()

    private fun render(): String? {
        if (diagnostics.isEmpty()) return null
        val fileName = testServices.moduleStructure.originalTestDataFiles.single().name

        data class DiagnosticData(val textRanges: List<TextRange>, val severity: String, val message: String)

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
            "/$fileName: ${it.severity}: ${it.message}"
        }.trimTrailingWhitespacesAndAddNewlineAtEOF()
    }

    fun addDiagnostics(info: FirOutputArtifact) {
        val frontendDiagnosticsPerFile =
            FirDiagnosticCollectorService(testServices).getFrontendDiagnosticsForModule(info)

        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            for (file in currentModule.files) {
                val firFile = info.mainFirFiles[file] ?: continue
                diagnostics.addAll(frontendDiagnosticsPerFile[firFile].map { it.diagnostic })
            }
        }
    }

    fun addDiagnostics(info: List<KtDiagnostic>) {
        diagnostics.addAll(info)
    }

    /**
     * Checks if the conversion has changed compared to the expected output. Returns true if the conversion has changed.
     * Does not perform any assertions.
     */
    fun resultHasChanged(): Boolean {
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile =
            testDataFile.parentFile.resolve("${testDataFile.nameWithoutExtension.removeSuffix(".fir")}${fileExtension}")

        val actualDiagnostics = render()

        // the golden file does not exist and diagnostics are empty
        if (!expectedFile.exists() && actualDiagnostics == null) {
            return false
        }

        val expectedDiagnostics = expectedFile.readText()

        return expectedDiagnostics.trimTrailingWhitespacesAndAddNewlineAtEOF() != actualDiagnostics?.trimTrailingWhitespacesAndAddNewlineAtEOF()
    }

    /**
     * Asserts equality of the diagnostics with the expected output.
     */
    fun assertEquality() {
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile =
            testDataFile.parentFile.resolve("${testDataFile.nameWithoutExtension.removeSuffix(".fir")}${fileExtension}")

        val expectedOutput = render()
        if (expectedOutput == null && !expectedFile.exists()) {
            return
        }

        testServices.assertions.assertEqualsToFile(expectedFile, expectedOutput ?: "")
    }
}

fun runChecks(testService: TestServices, vararg checks: () -> Unit) {
    val errors = checks.mapNotNull { runCatching { it() }.exceptionOrNull() }
    testService.assertions.failAll(errors)
}

