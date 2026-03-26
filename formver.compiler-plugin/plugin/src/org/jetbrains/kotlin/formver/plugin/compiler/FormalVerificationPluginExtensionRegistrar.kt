/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.plugin.compiler.fir.FirDeclarationVariablesResolver
import org.jetbrains.kotlin.formver.plugin.compiler.fir.FirReceiverResolver
import org.jetbrains.kotlin.formver.plugin.compiler.fir.FirTailsResolver
import org.jetbrains.kotlin.formver.plugin.compiler.fir.FirVariableDeclarationResolver
import org.jetbrains.kotlin.formver.plugin.compiler.locality.ConeLocalAttributeResolver
import org.jetbrains.kotlin.formver.plugin.compiler.locality.LocalTypeAttributeExtension

class FormalVerificationPluginExtensionRegistrar(private val config: PluginConfiguration) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        registerDiagnosticContainers(PluginErrors)
        +FirTailsResolver.getFactory()
        +FirReceiverResolver.getFactory()
        +FirDeclarationVariablesResolver.getFactory()
        +FirVariableDeclarationResolver.getFactory()
        +ConeLocalAttributeResolver.getFactory()
        +LocalTypeAttributeExtension.getFactory(config)
        +PluginAdditionalCheckers.getFactory(config)
    }
}
