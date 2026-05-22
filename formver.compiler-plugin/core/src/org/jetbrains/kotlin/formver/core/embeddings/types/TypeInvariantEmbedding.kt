/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.conversion.IntArrayElement
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.names.AnonymousBuiltinName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.PermExp

/**
 * An invariant for a type.
 *
 * These are different from invariants in general because they are parametrised by a variable of this type,
 * i.e. they can be seen as an `ExpEmbedding` with a hole.
 */
interface TypeInvariantEmbedding {
    fun fillHole(exp: ExpEmbedding): ExpEmbedding
}

fun List<TypeInvariantEmbedding>.fillHoles(exp: ExpEmbedding): List<ExpEmbedding> = map { it.fillHole(exp) }

data object FalseTypeInvariant : TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding = BooleanLit(false)
}

data class SubTypeInvariantEmbedding(val type: RuntimeTypeHolder) : TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding = Is(exp, type)
}

data class IfNonNullInvariant(val invariant: TypeInvariantEmbedding) : TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding =
        OperatorExpEmbeddings.Implies(NeCmp(exp, NullLit), invariant.fillHole(exp.withType(exp.type.getNonNullable())))
}

data class FieldEqualsInvariant(val field: FieldEmbedding, val comparedWith: ExpEmbedding) : TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding =
        EqCmp(PrimitiveFieldAccess(exp, field), comparedWith)
}

data class FieldAccessTypeInvariantEmbedding(val field: FieldEmbedding, val perm: PermExp) : TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding = FieldAccessPermissions(exp, field, perm)
}

//(forall i: Int :: 0 <= i && i < arrayLen(arr) ==>
//// The solver looks for 'arraySlot(arr, i)' in your code to trigger this permission
//{ arraySlot(arr, i).arrayElementVal }
//acc(arraySlot(arr, i).arrayElementVal)
//)
data object IntArrayAccessInvariantEmbedding : TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding {
        val index = PlaceholderVariableEmbedding(AnonymousBuiltinName(0), buildType { int() })
        return ForAllEmbedding(
            index,
            listOf(
                OperatorExpEmbeddings.Implies(
                    OperatorExpEmbeddings.And(
                        OperatorExpEmbeddings.LtIntInt(index, OperatorExpEmbeddings.intArraySize(exp)),
                        OperatorExpEmbeddings.LeIntInt(IntLit(0), index)
                    ),

                    AccEmbedding(
                        IntArrayElement,
                        OperatorExpEmbeddings.getSlot(exp, index),
                        PermExp.FullPerm(),
                    )
                )
            ),
            listOf()
        )
    }
}

// Note that at present, the predicate name and class name are the same.
// We may want to mangle it better down the line.
data class PredicateAccessTypeInvariantEmbedding(val predicateName: SymbolicName, val perm: PermExp) :
    TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding =
        PredicateAccessPermissions(predicateName, listOf(exp), perm)
}
