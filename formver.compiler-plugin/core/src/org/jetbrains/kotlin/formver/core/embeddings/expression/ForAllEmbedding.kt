/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.*
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.viper.NameResolver

class ForAllEmbedding(
    // TODO: support multiple variables
    val variable: VariableEmbedding,
    conditions: List<ExpEmbedding>,
    val triggerExpressions: List<ExpEmbedding> = emptyList(),
) : ExpEmbedding {

    val subexpressions: List<ExpEmbedding> = conditions

    override val type: TypeEmbedding
        get() = buildType { boolean() }

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode(javaClass.simpleName, subexpressions.map { it.debugTreeView })

    override fun children(): Sequence<ExpEmbedding> = subexpressions.asSequence()
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitForAllEmbedding(this)
}
