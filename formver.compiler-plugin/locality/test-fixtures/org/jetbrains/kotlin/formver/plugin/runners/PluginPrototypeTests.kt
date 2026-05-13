/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.runners

import org.jetbrains.kotlin.formver.plugin.services.LocalityExtensionRegistrarConfigurator
import org.jetbrains.kotlin.formver.plugin.services.PluginAnnotationsProvider
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.commonServicesConfigurationForCodegenAndDebugTest
import org.jetbrains.kotlin.test.configuration.configureCommonDiagnosticTestPaths
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.TestPhase


abstract class AbstractLocalityDiagnosticTest() : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LATEST_PHASE_IN_PIPELINE with TestPhase.FRONTEND
            +ENABLE_PLUGIN_PHASES
            +RENDER_DIAGNOSTICS_FULL_TEXT
            LANGUAGE with "+PropertyParamAnnotationDefaultTargetMode"
        }
        commonServicesConfigurationForCodegenAndDebugTest(FrontendKinds.FIR)

        facadeStep(::FirCliJvmFacade)
        handlersStep(FrontendKinds.FIR, compilationStage = CompilationStage.FIRST) {
            useHandlers(::FirDiagnosticsHandler)
        }
        enableMetaInfoHandler()

        configureFirParser(FirParser.LightTree)
        configureCommonDiagnosticTestPaths()

        useConfigurators(
            ::PluginAnnotationsProvider, ::LocalityExtensionRegistrarConfigurator
        )
    }

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }
}
