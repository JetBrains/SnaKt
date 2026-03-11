/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpWrapper
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.print
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.names.SimpleNameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.debugMangled

class PureLinearizerMisuseException(val offendingFunction: String) : IllegalStateException(offendingFunction)

/**
 * Linearization context that does not permit generation of statements.
 *
 * There are cases in Viper where we expect our result to be an expression by itself, for example when
 * processing preconditions, postconditions, and invariants. In those cases, generating statements
 * would be an error.
 */
data class PureLinearizer(
    override val source: KtSourceElement?,
    val state: SharedLinearizationState = SharedLinearizationState(FreshEntityProducer(::AnonymousVariableEmbedding)),
    private val ssaConverter: SsaConverter = SsaConverter(source),
    override val unfoldPolicy: UnfoldPolicy = UnfoldPolicy.UNFOLDING_IN,
    private val accessInvariants: MutableMap<SymbolicName, List<Exp.PredicateAccess>> = mutableMapOf()
) : LinearizationContext {

    override val logicOperatorPolicy: LogicOperatorPolicy
        get() = LogicOperatorPolicy.CONVERT_TO_EXPRESSION

    override fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R =
        copy(source = newSource).action()

    override fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding = state.freshAnonVar(type)

    override fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn {
        throw PureLinearizerMisuseException("withNewScopeToBlock")
    }

    override fun addStatement(buildStmt: LinearizationContext.() -> Stmt) {
        throw PureLinearizerMisuseException("addStatement")
    }

    // Nothing to do here, as an assignment is also added
    override fun addDeclaration(decl: Declaration) {}

    override fun store(lhs: VariableEmbedding, rhs: ExpEmbedding) {
        // It would be nicer to constraint this a bit further as this is a very special case
        ssaConverter.addAssignment(
            lhs.name,
            rhs.toViper(this)
        )
    }

    override fun addReturn(returnExp: ExpEmbedding, target: ReturnTarget) {
        ssaConverter.addReturn(returnExp.toViper(this))
    }

    override fun addBranch(
        condition: ExpEmbedding,
        thenBranch: ExpEmbedding,
        elseBranch: ExpEmbedding,
        type: TypeEmbedding,
        result: VariableEmbedding?
    ) {
        val conditionExp = condition.ignoringCastsAndMetaNodes().toViperBuiltinType(this)
        var resultThen: Exp? = null
        var resultElse: Exp? = null

        ssaConverter.branch(
            conditionExp,
            {
                if (result != null) {
                    resultThen = thenBranch.toViper(this)
                } else {
                    thenBranch.toViperUnusedResult(this)
                }
            },
            {
                if (result != null) {
                    resultElse = elseBranch.toViper(this)
                } else {
                    elseBranch.toViperUnusedResult(this)
                }
            })

        if (result != null) {
            if (resultThen == null || resultElse == null) throw SnaktInternalException(
                source,
                "Tried to translate an if-embedding missing a branch"
            )
            ssaConverter.addAssignment(result.name, Exp.TernaryExp(conditionExp, resultThen, resultElse))
        }
    }

    override fun handleFieldAccess(
        field: FieldEmbedding,
        receiver: ExpEmbedding,
        result: VariableEmbedding
    ) {
        val viperReceiver = receiver.toViper(this)
        if (viperReceiver !is Exp.LocalVar) throw SnaktInternalException(
            source,
            "Invalid receiver encountered in pure function"
        )
        val receiverWrapper = ExpWrapper(viperReceiver, receiver.type)
        val hierarchyPath = (receiver.type.pretype as? ClassTypeEmbedding)?.details?.hierarchyUnfoldPath(field)
        val predAccList = hierarchyPath?.map { it.predicateAccess(receiverWrapper, this) }?.toList() ?: emptyList()
        val completeAccList = (accessInvariants[viperReceiver.name] ?: emptyList()) + predAccList
        val primitiveAccess: Exp = Exp.FieldAccess(viperReceiver, field.toViper(), source.asPosition)
        val fieldAccessExpression = if (completeAccList.isEmpty()) {
            primitiveAccess
        } else {
            completeAccList.foldRight(primitiveAccess) { access, acc -> Exp.Unfolding(access, acc) }
        }
        ssaConverter.addAssignment(result.name, fieldAccessExpression)
        accessInvariants[ssaConverter.resolveVariableName(result.name)] = completeAccList
    }

    private fun ClassTypeEmbedding.predicateAccess(
        receiver: ExpEmbedding,
        ctx: LinearizationContext
    ): Exp.PredicateAccess =
        sharedPredicateAccessInvariant()?.fillHole(receiver)
            ?.pureToViper(toBuiltin = true, ctx.source) as? Exp.PredicateAccess
            ?: error("Attempt to unfold a predicate of ${name.debugMangled}.")

    private fun SymbolicName.stripSsaName() = if (this is SsaVariableName) this.baseName else this

    override fun addModifier(mod: StmtModifier) {
        throw PureLinearizerMisuseException("addModifier")
    }

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        ssaConverter.resolveVariableName(name)

    fun constructExpression(): Exp = ssaConverter.constructExpression()
}

fun ExpEmbedding.pureToViper(toBuiltin: Boolean, source: KtSourceElement? = null): Exp {
    try {
        val linearizer = PureLinearizer(source)
        return if (toBuiltin) toViperBuiltinType(linearizer) else toViper(linearizer)
    } catch (e: PureLinearizerMisuseException) {
        val catchNameResolver = SimpleNameResolver()
        val debugView = with(catchNameResolver) { debugTreeView.print() }
        val msg =
            "PureLinearizer used to convert non-pure ExpEmbedding; operation ${e.offendingFunction} is not supported in a pure context.\nEmbedding debug view:\n${debugView}"
        throw IllegalStateException(msg)
    }
}

fun List<ExpEmbedding>.pureToViper(toBuiltin: Boolean, source: KtSourceElement? = null): List<Exp> =
    map { it.pureToViper(toBuiltin, source) }