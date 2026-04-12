/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.asInfo
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ToViperBuiltinOnlyError
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.injectionOr
import org.jetbrains.kotlin.formver.viper.ast.Exp

interface Linearizable {
    fun toViper(ctx: LinearizationContext): Exp
    fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext)
    fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext)
    fun toViperBuiltinType(ctx: LinearizationContext): Exp
    fun toViperUnusedResult(ctx: LinearizationContext)
}

fun ExpEmbedding.toLinearizable(source: KtSourceElement? = null): Linearizable = accept(LinearizationVisitor(source))

internal fun defaultToViperBuiltinType(
    toViper: (LinearizationContext) -> Exp,
    type: TypeEmbedding,
    sourceRole: SourceRole?,
    ctx: LinearizationContext,
): Exp {
    val viperExp = toViper(ctx)
    val injection = type.injectionOr { return viperExp }
    return if (viperExp is Exp.DomainFuncApp && viperExp.function == injection.toRef)
        viperExp.args[0]
    else injection.fromRef(viperExp, pos = ctx.source.asPosition, info = sourceRole.asInfo)
}

abstract class DirectResultLinearizable(protected val exp: ExpEmbedding, private val visitor: LinearizationVisitor) : Linearizable {
    abstract override fun toViper(ctx: LinearizationContext): Exp

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        ctx.store(result, this)
    }

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (result != null) toViperStoringIn(result, ctx)
        else toViperUnusedResult(ctx)
    }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        defaultToViperBuiltinType(::toViper, exp.type, exp.sourceRole, ctx)

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        for (child in exp.children()) {
            child.accept(visitor).toViperUnusedResult(ctx)
        }
    }
}

abstract class StoredResultLinearizable(
    private val type: TypeEmbedding,
    private val sourceRole: SourceRole?,
) : Linearizable {
    constructor(exp: ExpEmbedding) : this(exp.type, exp.sourceRole)

    abstract override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext)

    override fun toViper(ctx: LinearizationContext): Exp {
        val variable = ctx.freshAnonVar(type)
        toViperStoringIn(variable, ctx)
        return variable.toViperExp(ctx)
    }

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (result != null) toViperStoringIn(result, ctx)
        else toViperUnusedResult(ctx)
    }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        defaultToViperBuiltinType(::toViper, type, sourceRole, ctx)

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        toViper(ctx)
    }
}

abstract class OptionalResultLinearizable(
    private val type: TypeEmbedding,
    private val sourceRole: SourceRole?,
) : Linearizable {
    constructor(exp: ExpEmbedding) : this(exp.type, exp.sourceRole)

    abstract override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext)

    override fun toViper(ctx: LinearizationContext): Exp {
        val variable = ctx.freshAnonVar(type)
        toViperMaybeStoringIn(variable, ctx)
        return variable.toViperExp(ctx)
    }

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        toViperMaybeStoringIn(result, ctx)
    }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        defaultToViperBuiltinType(::toViper, type, sourceRole, ctx)

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        toViperMaybeStoringIn(null, ctx)
    }
}

abstract class UnitResultLinearizable(
    private val type: TypeEmbedding,
    private val sourceRole: SourceRole?,
) : Linearizable {
    constructor(exp: ExpEmbedding) : this(exp.type, exp.sourceRole)

    abstract override fun toViperUnusedResult(ctx: LinearizationContext)

    override fun toViper(ctx: LinearizationContext): Exp {
        toViperUnusedResult(ctx)
        return RuntimeTypeDomain.unitValue(pos = ctx.source.asPosition)
    }

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        toViperUnusedResult(ctx)
    }

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (result != null) toViperStoringIn(result, ctx)
        else toViperUnusedResult(ctx)
    }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        defaultToViperBuiltinType(::toViper, type, sourceRole, ctx)
}

abstract class OnlyToBuiltinLinearizable(protected val exp: ExpEmbedding, private val visitor: LinearizationVisitor) : Linearizable {
    abstract override fun toViperBuiltinType(ctx: LinearizationContext): Exp

    override fun toViper(ctx: LinearizationContext): Exp = throw ToViperBuiltinOnlyError(exp)

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        ctx.store(result, this)
    }

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (result != null) toViperStoringIn(result, ctx)
        else toViperUnusedResult(ctx)
    }

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        for (child in exp.children()) {
            child.accept(visitor).toViperUnusedResult(ctx)
        }
    }
}
