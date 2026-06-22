/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

import org.jetbrains.kotlin.diagnostics.rendering.ContextIndependentParameterRenderer

/**
 * Renderer for [FunctionTypeFact].
 *
 * @param TypeFact the type-fact of the contract.
 * @param typeFactRenderer the renderer of the type-fact of the parameters of the contract.
 */
class FunctionTypeFactRenderer<TypeFact>(
    private val typeFactRenderer: ContextIndependentParameterRenderer<TypeFact>
) : ContextIndependentParameterRenderer<FunctionTypeFact<TypeFact>?> {
    override fun render(functionTypeFact: FunctionTypeFact<TypeFact>?): String =
        functionTypeFact?.renderContract() ?: "unknown"

    private fun FunctionTypeFact<TypeFact>.renderContract(): String =
        parameterTypeFacts.joinToString(prefix = "[", postfix = "]") { parameter ->
            parameter.renderElement()
        } + (resultFunctionTypeFact?.let { " -> ${it.renderContract()}" } ?: "")

    private fun FunctionTypeFact.ParameterTypeFact<TypeFact>.renderElement(): String {
        val renderedTypeFact = typeFactRenderer.render(typeFact)

        return functionTypeFact?.let { "($renderedTypeFact, ${it.renderContract()})" } ?: renderedTypeFact
    }
}
