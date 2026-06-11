/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

import org.jetbrains.kotlin.diagnostics.rendering.ContextIndependentParameterRenderer

/**
 * Renderer for [TypeContractFact].
 *
 * @param TypeFact the type-fact of the contract.
 * @param typeFactRenderer the renderer of the type-fact of the parameters of the contract.
 */
class TypeContractFactRenderer<TypeFact>(
    private val typeFactRenderer: ContextIndependentParameterRenderer<TypeFact>
) : ContextIndependentParameterRenderer<TypeContractFact<TypeFact>?> {
    override fun render(obj: TypeContractFact<TypeFact>?): String =
        obj?.renderContract() ?: "unknown"

    private fun TypeContractFact<TypeFact>.renderContract(): String =
        parameters.joinToString(prefix = "[", postfix = "]") { parameter ->
            parameter.renderElement()
        } + (result?.let { " -> ${it.renderContract()}" } ?: "")

    private fun TypeContractFact.ParameterType<TypeFact>.renderElement(): String {
        val renderedType = typeFactRenderer.render(type)

        return contract?.let { "($renderedType, ${it.renderContract()})" } ?: renderedType
    }
}
