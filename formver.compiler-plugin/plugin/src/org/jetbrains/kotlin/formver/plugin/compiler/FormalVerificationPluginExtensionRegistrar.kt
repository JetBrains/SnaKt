/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.core.diagnostics.ConversionErrors
import org.jetbrains.kotlin.formver.locality.plugin.LocalityExtensionRegistrar

class FormalVerificationPluginExtensionRegistrar(private val config: PluginConfiguration) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        registerDiagnosticContainers(PluginErrors)
        registerDiagnosticContainers(VerificationErrors)
        registerDiagnosticContainers(ConversionErrors)
        +PluginAdditionalCheckers.getFactory(config)
    }
}

@OptIn(ExperimentalCompilerApi::class)
fun ExtensionStorage.registerFormverExtensions(config: PluginConfiguration) {
    FirExtensionRegistrarAdapter.registerExtension(FormalVerificationPluginExtensionRegistrar(config))
    if (config.checkLocality) {
        FirExtensionRegistrarAdapter.registerExtension(LocalityExtensionRegistrar())
    }
}
