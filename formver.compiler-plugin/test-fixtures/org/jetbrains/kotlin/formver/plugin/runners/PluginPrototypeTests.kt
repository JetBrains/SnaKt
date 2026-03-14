/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.runners

import org.jetbrains.kotlin.formver.plugin.services.ConversionOnlyAfterAnalysisChecker
import org.jetbrains.kotlin.formver.plugin.services.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.CONVERSION_ONLY
import org.jetbrains.kotlin.formver.plugin.services.PluginAnnotationsProvider
import org.jetbrains.kotlin.formver.plugin.services.StdlibReplacementsProvider
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.runners.AbstractFirLightTreeDiagnosticsTest
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

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
 * Tests that already have CONVERSION_ONLY or UNIQUE_CHECK_ONLY are skipped
 * since they would run identically (or pointlessly) in this class.
 */
abstract class AbstractFirLightTreeFormVerPluginNoVerificationDiagnosticsTest : AbstractFirLightTreeDiagnosticsTest() {

    companion object {
        /** Directives whose presence means the test already runs without verification. */
        private val SKIP_DIRECTIVE_NAMES = listOf(
            FormVerDirectives.CONVERSION_ONLY,
            FormVerDirectives.UNIQUE_CHECK_ONLY,
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
            "Skipping: test already has CONVERSION_ONLY or UNIQUE_CHECK_ONLY directive",
        )
        super.runTest(filePath)
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonFirWithPluginFrontendConfiguration()
        builder.defaultDirectives {
            +CONVERSION_ONLY
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