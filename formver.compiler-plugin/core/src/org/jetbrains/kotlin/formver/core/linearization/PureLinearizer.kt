/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.print
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.names.SimpleNameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import kotlin.reflect.KProperty

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
    private val ssaConverterReference: SsaConverterReference = SsaConverterReference(SsaConverter())
) :
    LinearizationContext {
    var ssaConverter: SsaConverter by ssaConverterReference

    override val unfoldPolicy: UnfoldPolicy
        get() = UnfoldPolicy.UNFOLDING_IN

    override val logicOperatorPolicy: LogicOperatorPolicy
        get() = LogicOperatorPolicy.CONVERT_TO_EXPRESSION

    override fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R =
        copy(source = newSource).action()

    override fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding {
        throw PureLinearizerMisuseException("newVar")
    }

    override fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn {
        throw PureLinearizerMisuseException("withNewScopeToBlock")
    }

    override fun addStatement(buildStmt: LinearizationContext.() -> Stmt) {
        throw PureLinearizerMisuseException("addStatement")
    }

    // Nothing to do here, as an assignment is also added
    override fun addDeclaration(decl: Declaration) {}

    override fun addAssignment(lhs: ExpEmbedding, rhs: ExpEmbedding) {
        // It would be nicer to constraint this a bit further as this is a very special case
        ssaConverter.addAssignment(
            (lhs.ignoringMetaNodes() as VariableEmbedding).toLocalVarDecl(),
            rhs.toViper(this)
        )
    }

    override fun addReturn(returnExp: ExpEmbedding, target: ReturnTarget) {
        ssaConverter.addBody(returnExp.toViper(this))
    }

    override fun addBranch(
        condition: ExpEmbedding,
        thenBranch: ExpEmbedding,
        elseBranch: ExpEmbedding,
        type: TypeEmbedding,
        result: VariableEmbedding?
    ) {
        val startNode = this.ssaConverter
        val joinNode = startNode.createSibling()
        startNode.transferSuccessorsTo(joinNode)
        val conditionExp = condition.ignoringCastsAndMetaNodes().toViperBuiltinType(this)
        fun createBranch(cond: Exp) = createBranchLinearizer(
            branchCondition = cond,
            predecessors = mutableListOf(startNode),
            successors = mutableListOf(joinNode)
        )
        val thenLinearizer = createBranch(conditionExp)
        val elseLinearizer = createBranch(Exp.Not(conditionExp))
        val branchNodes = listOf(thenLinearizer.ssaConverter, elseLinearizer.ssaConverter)
        startNode.successors.addAll(branchNodes)
        joinNode.predecessors.addAll(branchNodes)
        this.ssaConverter = joinNode
        thenBranch.toViperUnusedResult(thenLinearizer)
        elseBranch.toViperUnusedResult(elseLinearizer)
    }

    /**
     * Creates a new converter with the same properties as the original.
     */
    private fun SsaConverter.createSibling(
        localCondition: Exp? = this.localBranchingCondition,
        globalCondition: Exp? = this.globalBranchingCondition
    ) = SsaConverter(
        variableIndex = this.variableIndex,
        localBranchingCondition = localCondition,
        globalBranchingCondition = globalCondition
    )

    /**
     * Moves all successors from this node to the target node, updating back-references.
     */
    private fun SsaConverter.transferSuccessorsTo(target: SsaConverter) {
        if (this.successors.isEmpty()) return
        this.successors.forEach { successor ->
            successor.predecessors.remove(this)
            successor.predecessors.add(target)
        }
        target.successors.addAll(this.successors)
        this.successors.clear()
    }

    /**
     * Creates an SSAConverter for a specific branch
     */
    private fun createBranchLinearizer(
        branchCondition: Exp,
        predecessors: MutableList<SsaConverter>,
        successors: MutableList<SsaConverter>
    ): PureLinearizer {
        val parentGlobal = this.ssaConverter.globalBranchingCondition
        val newGlobal = if (parentGlobal != null) {
            listOf(branchCondition, parentGlobal).toConjunction()
        } else {
            branchCondition
        }
        val branchSsa = SsaConverter(
            variableIndex = this.ssaConverter.variableIndex,
            predecessors = predecessors,
            successors = successors,
            localBranchingCondition = branchCondition,
            globalBranchingCondition = newGlobal
        )
        return PureLinearizer(source, SsaConverterReference(branchSsa))
    }

    override fun addModifier(mod: StmtModifier) {
        throw PureLinearizerMisuseException("addModifier")
    }

    override fun resolveVariableName(name: SymbolicName): SymbolicName = ssaConverter.resolveVariableName(name)
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

data class SsaConverterReference(var value: SsaConverter) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): SsaConverter = this.value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: SsaConverter) {
        this.value = value
    }
}