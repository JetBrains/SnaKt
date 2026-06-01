/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

import org.jetbrains.kotlin.diagnostics.rendering.ContextIndependentParameterRenderer

class TypeContractRenderer<Type>(
    private val typeRenderer: ContextIndependentParameterRenderer<Type>
) : ContextIndependentParameterRenderer<FunctionType<Type>?> {
    override fun render(obj: FunctionType<Type>?): String =
        obj?.renderContract() ?: "unknown"

    private fun FunctionType<Type>.renderContract(): String =
        parameters.joinToString(prefix = "[", postfix = "]") { parameter ->
            parameter.renderElement()
        } + (result?.let { " -> ${it.renderContract()}" } ?: "")

    private fun FunctionType.ParameterType<Type>.renderElement(): String {
        val renderedType = typeRenderer.render(type)

        return contract?.let { "($renderedType, ${it.renderContract()})" } ?: renderedType
    }
}
