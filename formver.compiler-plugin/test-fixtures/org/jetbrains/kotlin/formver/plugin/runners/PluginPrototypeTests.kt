/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.runners

import org.jetbrains.kotlin.formver.plugin.services.*
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.NEVER_VALIDATE
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.commonServicesConfigurationForCodegenAndDebugTest
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractFirLightTreeDiagnosticsTest
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.*

abstract class AbstractFirLightTreeFormVerPluginDiagnosticsTest : AbstractFirLightTreeDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonFirWithPluginFrontendConfiguration()
    }

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }
}

/**
 * Runs verification tests in conversion-only mode: verification is skipped,
 * consistency checking still runs.  Used in bothModes to double-check
 * that verification tests also pass as conversion-only tests.
 *
 * Tests that already have NEVER_VALIDATE or UNIQUE_CHECK_ONLY are skipped
 * since they would run identically (or pointlessly) in this class.
 */
abstract class AbstractFirLightTreeFormVerPluginNoVerificationDiagnosticsTest : AbstractFirLightTreeDiagnosticsTest() {

    companion object {
        /** Directives whose presence means the test already runs without verification. */
        private val SKIP_DIRECTIVE_NAMES = listOf(
            FormVerDirectives.NEVER_VALIDATE,
            FormVerDirectives.UNIQUE_CHECK_ONLY,
            FormVerDirectives.LOCALITY_CHECK_ONLY,
        ).map { "// ${it.name}" }
    }

    override fun runTest(filePath: String) {
        // Directives aren't parsed yet at this point in the test lifecycle,
        // so we check the file text directly against known directive names.
        val hasRedundantDirective = java.io.File(filePath).useLines { lines ->
            lines.any { line -> SKIP_DIRECTIVE_NAMES.any { line.trimStart().startsWith(it) } }
        }
        org.junit.jupiter.api.Assumptions.assumeFalse(
            hasRedundantDirective,
            "Skipping: test already has NEVER_VALIDATE or UNIQUE_CHECK_ONLY directive",
        )
        super.runTest(filePath)
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonFirWithPluginFrontendConfiguration()
        builder.defaultDirectives {
            +NEVER_VALIDATE
        }
    }

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }
}

fun TestConfigurationBuilder.commonFirWithPluginFrontendConfiguration() {
    defaultDirectives {
        +ENABLE_PLUGIN_PHASES
        +RENDER_DIAGNOSTICS_FULL_TEXT
        LANGUAGE with "+PropertyParamAnnotationDefaultTargetMode"

        +JvmEnvironmentConfigurationDirectives.FULL_JDK
    }

    useAdditionalSourceProviders(::StdlibReplacementsProvider)

    useConfigurators(
        ::PluginAnnotationsProvider,
        ::ExtensionRegistrarConfigurator
    )

    useAfterAnalysisCheckers(::ConversionOnlyAfterAnalysisChecker)
}


enum class TestMode {
    CHECK_CONVERSION, UPDATE, FULL
}

fun getTestMode(): TestMode {
    return when (System.getProperty("formver.testMode")) {
        "CHECK_CONVERSION" -> TestMode.CHECK_CONVERSION
        "UPDATE" -> TestMode.UPDATE
        "FULL" -> TestMode.FULL
        null -> TestMode.FULL
        else -> throw IllegalStateException("Unknown test mode: ${System.getProperty("formver.testMode")}")
    }
}


/**
 * Runs tests with different modes based on the formver.testMode system property.
 * This test driver divides each test into two phases: conversion and verification.
 *
 * The conversion phase includes: uniqueness, conversion, purity, and viper consistency
 * The verification phase includes: viper verification
 *
 * The following modes exist:
 *
 * - CHECK_CONVERSION: runs only the conversion phase.
 * Use case: Check that code modification did not change the output of the conversion
 *
 * - UPDATE: runs the conversion and for those tests that changed, run the verification as well
 * Use case: After modifications check if the output is correct
 *
 * - FULL: runs all conversions and verification
 * Use case: Use before open PR and in the CI
 */
abstract class AbstractPhasedDiagnosticTest() : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LATEST_PHASE_IN_PIPELINE with TestPhase.FRONTEND
            +ENABLE_PLUGIN_PHASES
            +RENDER_DIAGNOSTICS_FULL_TEXT
            +JvmEnvironmentConfigurationDirectives.FULL_JDK
            LANGUAGE with "+PropertyParamAnnotationDefaultTargetMode"
        }
        commonServicesConfigurationForCodegenAndDebugTest(FrontendKinds.FIR)

        facadeStep(::FirCliJvmFacade)
        handlersStep(FrontendKinds.FIR, compilationStage = CompilationStage.FIRST) {
            useHandlers(::AfterConversionHandler)
        }

        // These special services are used to divide the conversion and verification diagnostics/tags
        useAdditionalService(::DiagnosticsCollector)
        useAdditionalService(::TagCollector)

        // This facade might verify the programs. This depends on the testMode and the result of the conversion check.
        facadeStep(::ViperProgramVerificationFacade)
        handlersStep(
            FrontendKinds.FIR, compilationStage = CompilationStage.FIRST
        ) {
            useHandlers(::ViperResultHandler)
        }

        configureFirParser(FirParser.LightTree)

        useAdditionalSourceProviders(::StdlibReplacementsProvider)

        useConfigurators(
            ::PluginAnnotationsProvider, ::ExtensionRegistrarConfigurator
        )
    }

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }
}

fun runChecks(testService: TestServices, vararg checks: () -> Unit) {
    val errors = checks.mapNotNull { runCatching { it() }.exceptionOrNull() }
    testService.assertions.failAll(errors)
}