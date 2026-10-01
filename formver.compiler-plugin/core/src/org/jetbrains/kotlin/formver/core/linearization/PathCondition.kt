/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction

/**
 * The condition under which an SSA block is reached.
 */
sealed interface PathCondition {
    /** The block follows a `return` on every path. */
    data object Unreachable : PathCondition

    /**
     * The block is reached when all [conjuncts] hold.
     *
     * Conjuncts are kept as a list so that joins can find the shared prefix of both
     * predecessors instead of building nested disjunctions.
     */
    data class Reachable(val conjuncts: List<Exp>) : PathCondition {
        val exp: Exp = conjuncts.toConjunction()
    }

    fun and(condition: Exp): PathCondition = when (this) {
        Unreachable -> Unreachable
        is Reachable -> Reachable(conjuncts + condition)
    }

    companion object {
        val Always = Reachable(emptyList())

        /**
         * The condition of a join of [left] and [right].
         *
         * `(p && c) || (p && !c)` folds to `p`, and `p || (p && q)` to `p`, so the condition
         * stays linear in the number of preceding branches.
         */
        fun or(left: PathCondition, right: PathCondition): PathCondition = when {
            left !is Reachable -> right
            right !is Reachable -> left
            else -> {
                val prefixLength = left.conjuncts.zip(right.conjuncts).takeWhile { (l, r) -> l == r }.size
                val prefix = left.conjuncts.take(prefixLength)
                val leftRest = left.conjuncts.drop(prefixLength)
                val rightRest = right.conjuncts.drop(prefixLength)
                when {
                    leftRest.isEmpty() || rightRest.isEmpty() -> Reachable(prefix)
                    areComplementary(leftRest, rightRest) -> Reachable(prefix)
                    else -> Reachable(prefix + Exp.Or(leftRest.toConjunction(), rightRest.toConjunction()))
                }
            }
        }

        private fun areComplementary(left: List<Exp>, right: List<Exp>): Boolean {
            val l = left.singleOrNull() ?: return false
            val r = right.singleOrNull() ?: return false
            return l == Exp.Not(r) || r == Exp.Not(l)
        }
    }
}
