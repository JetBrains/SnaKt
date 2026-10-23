/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.core.domains.Injection
import org.jetbrains.kotlin.formver.core.domains.viperType
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.injectionOrNull
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * The Viper representation in which the two operands of a comparison are compared.
 *
 * A Viper `==` requires both of its operands to have the same sort, so a comparison has to settle
 * on one representation and linearize both operands into it.
 */
sealed interface ComparisonRepresentation {
    /** Linearizes [operand], the embedding type of which is [type], into this representation. */
    fun operandToViper(operand: Linearizable, type: TypeEmbedding, ctx: LinearizationContext): Exp

    /**
     * Compare the operands as the builtin type that [injection] maps to `Ref`.
     *
     * Value comparisons select this representation only when both operand types use [injection].
     * Keeping those comparisons on builtin values avoids relying on the injection's round-trip
     * axiom to relate reference equality to builtin equality; that axiom needs a `fromRef` trigger.
     * Reference identity instead uses [Refs], even when both operands share an injection.
     */
    data class Builtin(val injection: Injection) : ComparisonRepresentation {
        override fun operandToViper(operand: Linearizable, type: TypeEmbedding, ctx: LinearizationContext): Exp {
            // `toViperBuiltinType` unwraps using the operand type's injection, which must match
            // the injection selected for both operands.
            check(type.injectionOrNull == injection) {
                "Comparison operand is represented as ${type.injectionOrNull.viperType}, expected ${injection.viperType}."
            }
            return operand.toViperBuiltinType(ctx)
        }
    }

    /**
     * Compare the operands as `Ref`s.
     *
     * The only representation available when the operands have no builtin one in common: class
     * types, `Any` and every nullable type are represented as `Ref` and nothing else.
     */
    data object Refs : ComparisonRepresentation {
        override fun operandToViper(operand: Linearizable, type: TypeEmbedding, ctx: LinearizationContext): Exp =
            operand.toViper(ctx)
    }

    companion object {
        /** The representation that operands of types [left] and [right] have in common. */
        fun shared(left: TypeEmbedding, right: TypeEmbedding): ComparisonRepresentation {
            val injection = left.injectionOrNull ?: return Refs
            return if (injection == right.injectionOrNull) Builtin(injection) else Refs
        }
    }
}
