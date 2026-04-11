/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.*
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.ast.Exp

data class Old(val inner: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = inner.type

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode("Old", inner.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitOld(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(inner)
}
