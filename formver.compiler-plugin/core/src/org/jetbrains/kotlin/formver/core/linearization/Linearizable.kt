/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.asInfo
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ToViperBuiltinOnlyError
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.injectionOr
import org.jetbrains.kotlin.formver.viper.ast.Exp

interface Linearizable {
    fun toViper(ctx: LinearizationContext): Exp
    fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext)
    fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext)
    fun toViperBuiltinType(ctx: LinearizationContext): Exp
    fun toViperUnusedResult(ctx: LinearizationContext)
}

fun ExpEmbedding.linearize(): Linearizable = accept(LinearizationVisitor)

fun List<ExpEmbedding>.linearizeToViper(ctx: LinearizationContext): List<Exp> = map { it.linearize().toViper(ctx) }

internal fun defaultToViperBuiltinType(toViper: (LinearizationContext) -> Exp, exp: ExpEmbedding, ctx: LinearizationContext): Exp {
    val viperExp = toViper(ctx)
    val injection = exp.type.injectionOr { return viperExp }
    return if (viperExp is Exp.DomainFuncApp && viperExp.function == injection.toRef)
        viperExp.args[0]
    else injection.fromRef(viperExp, pos = ctx.source.asPosition, info = exp.sourceRole.asInfo)
}

abstract class DirectResultLinearizable(protected val exp: ExpEmbedding) : Linearizable {
    abstract override fun toViper(ctx: LinearizationContext): Exp

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        ctx.store(result, exp)
    }

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (result != null) toViperStoringIn(result, ctx)
        else toViperUnusedResult(ctx)
    }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        defaultToViperBuiltinType(::toViper, exp, ctx)

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        for (child in exp.children()) {
            child.linearize().toViperUnusedResult(ctx)
        }
    }
}

abstract class StoredResultLinearizable(protected val exp: ExpEmbedding) : Linearizable {
    abstract override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext)

    override fun toViper(ctx: LinearizationContext): Exp {
        val variable = ctx.freshAnonVar(exp.type)
        toViperStoringIn(variable, ctx)
        return variable.linearize().toViper(ctx)
    }

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (result != null) toViperStoringIn(result, ctx)
        else toViperUnusedResult(ctx)
    }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        defaultToViperBuiltinType(::toViper, exp, ctx)

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        toViper(ctx)
    }
}

abstract class OptionalResultLinearizable(protected val exp: ExpEmbedding) : Linearizable {
    abstract override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext)

    override fun toViper(ctx: LinearizationContext): Exp {
        val variable = ctx.freshAnonVar(exp.type)
        toViperMaybeStoringIn(variable, ctx)
        return variable.linearize().toViper(ctx)
    }

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        toViperMaybeStoringIn(result, ctx)
    }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        defaultToViperBuiltinType(::toViper, exp, ctx)

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        toViperMaybeStoringIn(null, ctx)
    }
}

abstract class NoResultLinearizable(protected val exp: ExpEmbedding) : Linearizable {
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
        defaultToViperBuiltinType(::toViper, exp, ctx)
}

abstract class UnitResultLinearizable(protected val exp: ExpEmbedding) : Linearizable {
    abstract fun toViperSideEffects(ctx: LinearizationContext)

    override fun toViper(ctx: LinearizationContext): Exp {
        toViperSideEffects(ctx)
        return RuntimeTypeDomain.unitValue(pos = ctx.source.asPosition)
    }

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        ctx.store(result, exp)
    }

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (result != null) toViperStoringIn(result, ctx)
        else toViperUnusedResult(ctx)
    }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        defaultToViperBuiltinType(::toViper, exp, ctx)

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        toViper(ctx)
    }
}

abstract class OnlyToBuiltinLinearizable(protected val exp: ExpEmbedding) : Linearizable {
    abstract override fun toViperBuiltinType(ctx: LinearizationContext): Exp

    override fun toViper(ctx: LinearizationContext): Exp = throw ToViperBuiltinOnlyError(exp)

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        ctx.store(result, exp)
    }

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (result != null) toViperStoringIn(result, ctx)
        else toViperUnusedResult(ctx)
    }

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        for (child in exp.children()) {
            child.linearize().toViperUnusedResult(ctx)
        }
    }
}
