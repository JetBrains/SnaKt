/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.InternalDiagnosticFactoryMethod
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.formver.common.*
import org.jetbrains.kotlin.formver.plugin.compiler.*
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.toKtPsiSourceElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VerificationDiagnosticsTest {
    @Test
    fun onlyWarningContainerIsRegisteredAtEitherSeverity() {
        for (severity in VerificationErrorSeverity.entries) {
            val config = PluginConfiguration(
                LogLevel.ONLY_WARNINGS, ErrorStyle.USER_FRIENDLY, UnsupportedFeatureBehaviour.THROW_EXCEPTION,
                TargetsSelection.ALL_TARGETS, TargetsSelection.ALL_TARGETS, false, false, false, severity,
            )
            val containers = FormalVerificationPluginExtensionRegistrar(config).configure().diagnosticsContainers
            assertEquals(1, containers.count { it === VerificationErrors })
            assertFalse(containers.any { it === StrictVerificationErrors })
            assertTrue(containers.any { it === PluginErrors })
        }
    }

    @Test
    @OptIn(InternalDiagnosticFactoryMethod::class)
    fun bothSeveritiesKeepNamesAndRenderEveryFailedProofVariant() {
        val disposable = Disposer.newDisposable()
        try {
            val environment = KotlinCoreEnvironment.createForTests(
                disposable, CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES,
            )
            val source = KtPsiFactory(environment.project).createFile("fun test() = Unit").toKtPsiSourceElement()
            val positioning = SourceElementPositioningStrategies.DEFAULT
            val language = LanguageVersionSettingsImpl.DEFAULT
            val names = listOf(
                "VIPER_VERIFICATION_ERROR", "UNEXPECTED_RETURNED_VALUE", "CONDITIONAL_EFFECT_ERROR",
                "POSSIBLE_INDEX_OUT_OF_BOUND", "INVALID_SUBLIST_RANGE",
            )
            val messages = listOf(
                "Viper verification error: proof", "Function may return a null value.",
                "Cannot verify that if condition then effect.",
                "Invalid index for list, the index may be negative.",
                "Invalid sub-list range for list, the range may be reversed.",
            )
            for (severity in VerificationErrorSeverity.entries) {
                val factories = VerificationDiagnostics.of(severity)
                assertSame(
                    if (severity == VerificationErrorSeverity.WARNING) VerificationErrors else StrictVerificationErrors,
                    factories,
                )
                val diagnostics = listOf<KtDiagnostic?>(
                    factories.VIPER_VERIFICATION_ERROR.on(source, "proof", positioning, language),
                    factories.UNEXPECTED_RETURNED_VALUE.on(source, "null", positioning, language),
                    factories.CONDITIONAL_EFFECT_ERROR.on(source, "condition", "effect", positioning, language),
                    factories.POSSIBLE_INDEX_OUT_OF_BOUND.on(source, "list", "negative", positioning, language),
                    factories.INVALID_SUBLIST_RANGE.on(source, "list", "reversed", positioning, language),
                ).map { requireNotNull(it) }
                assertEquals(names, diagnostics.map { it.factory.name })
                assertTrue(diagnostics.all {
                    it.severity == if (severity == VerificationErrorSeverity.WARNING) Severity.WARNING else Severity.ERROR
                })
                assertEquals(messages, diagnostics.map {
                    requireNotNull(FormalVerificationPluginErrorMessages.MAP[it.factory]).render(it)
                })
            }
            assertEquals(Severity.ERROR, VerificationErrors.CONSISTENCY.severity)
            assertEquals(Severity.ERROR, PluginErrors.INTERNAL_ERROR.severity)
        } finally {
            ApplicationManager.getApplication().runWriteAction { Disposer.dispose(disposable) }
        }
    }
}
