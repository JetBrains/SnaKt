/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.runners

import org.jetbrains.kotlin.formver.common.services.DiagnosticsCollector
import org.jetbrains.kotlin.formver.common.services.TagCollector
import org.jetbrains.kotlin.formver.common.services.runChecks
import org.jetbrains.kotlin.formver.plugin.services.LocalityExtensionRegistrarConfigurator
import org.jetbrains.kotlin.formver.plugin.services.PluginAnnotationsProvider
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.commonServicesConfigurationForCodegenAndDebugTest
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.*


class ConversionDiagnosticsCollector(testServices: TestServices) : DiagnosticsCollector(testServices) {
    override val fileExtension: String = ".fir.diag.txt"
}

class LocalityTagsCollector(testServices: TestServices) : TagCollector(testServices)


val TestServices.conversionDiagnosticsCollector: ConversionDiagnosticsCollector by TestServices.testServiceAccessor()

val TestServices.tagCollector: LocalityTagsCollector by TestServices.testServiceAccessor()


class DiagnosticHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    override fun processModule(
        module: TestModule, info: FirOutputArtifact
    ) {
        testServices.conversionDiagnosticsCollector.addDiagnostics(info)

        val frontendDiagnosticsPerFile =
            FirDiagnosticCollectorService(testServices).getFrontendDiagnosticsForModule(info)

        module.files.forEach { file ->
            val testFile = info.allFirFiles[file]!!
            val diagnostics = frontendDiagnosticsPerFile[testFile]
            val simpleDiagnostics = diagnostics.map { it.diagnostic }
            testServices.tagCollector.reportDiagnostics(file, simpleDiagnostics)
        }

        runChecks(
            testServices,
            { testServices.conversionDiagnosticsCollector.assertEquality() },
            { testServices.tagCollector.assertEqual() },
        )
    }
}



abstract class AbstractLocalityDiagnosticTest() : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LATEST_PHASE_IN_PIPELINE with TestPhase.FRONTEND
            +ENABLE_PLUGIN_PHASES
            +RENDER_DIAGNOSTICS_FULL_TEXT
            LANGUAGE with "+PropertyParamAnnotationDefaultTargetMode"
        }
        commonServicesConfigurationForCodegenAndDebugTest(FrontendKinds.FIR)

        useAdditionalService(::ConversionDiagnosticsCollector)
        useAdditionalService(::LocalityTagsCollector)

        facadeStep(::FirCliJvmFacade)
        handlersStep(FrontendKinds.FIR, compilationStage = CompilationStage.FIRST) {
            useHandlers(::DiagnosticHandler)
        }

        configureFirParser(FirParser.LightTree)

        useConfigurators(
            ::PluginAnnotationsProvider, ::LocalityExtensionRegistrarConfigurator
        )
    }

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }
}
