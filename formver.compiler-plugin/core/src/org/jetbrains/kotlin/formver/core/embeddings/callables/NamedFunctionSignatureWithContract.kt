/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.conversion.stdLibPostconditions
import org.jetbrains.kotlin.formver.core.conversion.stdLibPreconditions
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding

interface NamedFunctionSignatureWithContract : NamedFunctionSignature {
    /**
     * Preconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    val preconditions: List<ExpEmbedding>

    /**
     * Postconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    val postconditions: List<ExpEmbedding>

    val declarationSource: KtSourceElement?
}


// Container for the generated conditions
data class FunctionContract(
    val preconditions: List<ExpEmbedding>, val postconditions: List<ExpEmbedding>
)

// Entry point extension method
fun NamedFunctionSignature.buildConditions(
    typeResolver: TypeResolver, block: FunctionConditionBuilder.() -> Unit
): FunctionContract {
    return FunctionConditionBuilder(this, typeResolver).apply(block).build()
}

class FunctionConditionBuilder(
    private val signature: NamedFunctionSignature, private val typeResolver: TypeResolver
) {
    private val preconditions = mutableListOf<ExpEmbedding>()
    private val postconditions = mutableListOf<ExpEmbedding>()

    // Granular Scopes
    fun preconditions(block: PreconditionScope.() -> Unit) {
        PreconditionScope(signature, typeResolver, preconditions).apply(block)
    }

    fun postconditions(block: PostconditionScope.() -> Unit) {
        PostconditionScope(signature, typeResolver, postconditions).apply(block)
    }


    fun addPreconditions(list: List<ExpEmbedding>) = preconditions.addAll(list)
    fun addPostconditions(list: List<ExpEmbedding>) = postconditions.addAll(list)

    fun userFunctionPreconditions() {
        preconditions {
            args {
                pureInvariants()
                provenInvariants()
                accessInvariants()
                if (variable.isUnique) uniquePredicateInvariants()
            }
            stdLib()
        }
    }

    fun userFunctionPostcondition() {
        postconditions {
            args {
                accessInvariants()
                pureInvariants()
                if (variable.isBorrowed && variable.isUnique) uniquePredicateInvariants()
            }
            returns {
                pureInvariants()
                provenInvariants()
                if (!signature.isPure) {
                    accessInvariants()
                    if (signature.callableType.returnsUnique) {
                        uniquePredicateInvariants()
                    }
                }
            }
            stdLib()
        }
    }

    fun userFunctionContract() {
        userFunctionPreconditions()
        userFunctionPostcondition()
    }

    fun build(): FunctionContract = FunctionContract(preconditions, postconditions)
}

class PreconditionScope(
    private val signature: NamedFunctionSignature,
    private val typeResolver: TypeResolver,
    private val list: MutableList<ExpEmbedding>
) {
    fun args(block: VariableScope.() -> Unit) {
        signature.formalArgs.forEach { variable -> VariableScope(variable, list, typeResolver).apply(block) }
    }

    fun stdLib() {
        list.addAll(signature.stdLibPreconditions(typeResolver))
    }
}


class VariableScope(
    val variable: VariableEmbedding,
    private val list: MutableList<ExpEmbedding>,
    private val typeResolver: TypeResolver
) {
    fun pureInvariants() = list.addAll(variable.pureInvariants())
    fun accessInvariants() = list.addAll(variable.accessInvariants(typeResolver))
    fun provenInvariants() = list.addAll(variable.provenInvariants())

    fun uniquePredicateInvariants() {
        variable.type.uniquePredicateAccessInvariant(typeResolver)?.fillHole(variable)?.let { inv ->
            list.add(inv)
        }
    }
}

class PostconditionScope(
    private val signature: NamedFunctionSignature,
    private val typeResolver: TypeResolver,
    private val list: MutableList<ExpEmbedding>
) {
    fun args(block: VariableScope.() -> Unit) {
        signature.formalArgs.forEach { variable -> VariableScope(variable, list, typeResolver).apply(block) }
    }

    fun returns(block: VariableScope.() -> Unit) {
        VariableScope(signature.returns, list, typeResolver).apply(block)
    }

    fun stdLib() {
        list.addAll(signature.stdLibPostconditions(signature.returns, typeResolver))
    }
}

