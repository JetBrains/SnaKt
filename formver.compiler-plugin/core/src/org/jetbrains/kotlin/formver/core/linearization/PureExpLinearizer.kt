/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FieldAccess
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.print
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.predicateAccess
import org.jetbrains.kotlin.formver.names.SimpleNameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

class PureExpLinearizerMisuseException(val offendingFunction: String) : IllegalStateException(offendingFunction)

/**
 * Linearization context linearizing a pure ExpEmbedding into a Viper expression.
 *
 * There are cases in Viper where we expect our result to be an expression by itself, for example when
 * processing preconditions, postconditions, and invariants. Compared to the [PureFunBodyLinearizer],
 * this linearizer is highly restrictive on what embeddings are supported and is used to translate
 * specifications made in Kotlin into Viper expressions.
 */
data class PureExpLinearizer(
    override val source: KtSourceElement?
) : LinearizationContext {

    override val logicOperatorPolicy: LogicOperatorPolicy
        get() = LogicOperatorPolicy.CONVERT_TO_EXPRESSION

    override fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R =
        copy(source = newSource).action()

    override fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding {
        throw PureExpLinearizerMisuseException("freshAnonVar")
    }

    override fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn {
        throw PureExpLinearizerMisuseException("withNewScopeToBlock")
    }

    override fun addStatement(buildStmt: LinearizationContext.() -> Stmt) {
        throw PureExpLinearizerMisuseException("addStatement")
    }

    override fun addDeclaration(decl: Declaration) {
        throw PureExpLinearizerMisuseException("addDeclaration")
    }

    override fun store(lhs: VariableEmbedding, rhs: ExpEmbedding) {
        throw PureExpLinearizerMisuseException("store")
    }

    override fun addReturn(returnExp: ExpEmbedding, target: ReturnTarget) {
        throw PureExpLinearizerMisuseException("addReturn")
    }

    override fun addBranch(
        condition: ExpEmbedding,
        thenBranch: ExpEmbedding,
        elseBranch: ExpEmbedding,
        type: TypeEmbedding,
        result: VariableEmbedding?
    ) {
        throw PureExpLinearizerMisuseException("addBranch")
    }

    override fun addFieldAccessStoringIn(access: FieldAccess, result: VariableEmbedding) {
        throw PureExpLinearizerMisuseException("addFieldAccessWithResult")
    }

    override fun addFieldAccess(access: FieldAccess): Exp =
        access.unfoldingInImpl()


    override fun addModifier(mod: StmtModifier) {
        throw PureExpLinearizerMisuseException("addModifier")
    }

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        name

    private fun FieldAccess.unfoldingInImpl(): Exp {
        val hierarchyPath = receiver.type.hierarchyUnfoldPath(field)
        val primitiveAccess: Exp =
            Exp.FieldAccess(receiver.toViper(this@PureExpLinearizer), field.toViper(), source.asPosition)
        if (hierarchyPath == null) return primitiveAccess
        return hierarchyPath.toList().foldRight(primitiveAccess) { classType, acc ->
            val predAcc = classType.predicateAccess(receiver, source)
            Exp.Unfolding(predAcc, acc)
        }
    }
}

fun ExpEmbedding.pureToViper(toBuiltin: Boolean, source: KtSourceElement? = null): Exp {
    try {
        val linearizer = PureExpLinearizer(source)
        return if (toBuiltin) toViperBuiltinType(linearizer) else toViper(linearizer)
    } catch (e: PureExpLinearizerMisuseException) {
        val catchNameResolver = SimpleNameResolver()
        val debugView = with(catchNameResolver) { debugTreeView.print() }
        val msg =
            "PureLinearizer used to convert non-pure ExpEmbedding; operation ${e.offendingFunction} is not supported in a pure context.\nEmbedding debug view:\n${debugView}"
        throw IllegalStateException(msg)
    }
}

fun List<ExpEmbedding>.pureToViper(toBuiltin: Boolean, source: KtSourceElement? = null): List<Exp> =
    map { it.pureToViper(toBuiltin, source) }