package org.jetbrains.kotlin.formver.plugin.services

import org.jetbrains.kotlin.formver.plugin.runners.TestMode
import org.jetbrains.kotlin.formver.plugin.runners.getTestMode
import org.jetbrains.kotlin.formver.plugin.runners.runChecks
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Registers the diagnostics and tags
 * If necessary, asserts equality of the conversion output.
 */
class AfterConversionHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    override fun processModule(
        module: TestModule, info: FirOutputArtifact
    ) {
        testServices.diagnosticsCollector.addConversionDiagnostics(info)

        val frontendDiagnosticsPerFile =
            FirDiagnosticCollectorService(testServices).getFrontendDiagnosticsForModule(info)

        module.files.forEach { file ->
            val testFile = info.allFirFiles[file]!!
            val diagnostics = frontendDiagnosticsPerFile[testFile]
            val simpleDiagnostics = diagnostics.map { it.diagnostic }
            testServices.tagCollector.reportDiagnostics(file, simpleDiagnostics)
        }

        val mode = getTestMode()
        if (mode == TestMode.CHECK_CONVERSION) {
            runChecks(
                testServices,
                { testServices.diagnosticsCollector.assertConversion() },
                { testServices.tagCollector.assertConversion() })

        }
    }
}