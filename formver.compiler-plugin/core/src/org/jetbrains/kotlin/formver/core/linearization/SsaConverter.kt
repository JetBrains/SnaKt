package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Type

class SsaConverter(
    val source: KtSourceElement? = null,
) {
    private var head: SsaBlockNode = SsaBlockNode(SsaStartNode(), Exp.BoolLit(true))
    private val ssaAssignments: MutableList<Pair<SsaVariableName, Exp>> = mutableListOf()
    private val returnExpressions: MutableList<Pair<Exp, Exp>> = mutableListOf()

    // Using a let-bound variable under one of the conditions requires the listed predicates to be unfolded.
    // The conditions of each entry are mutually exclusive and together exhaustive.
    private val accessDependencies: MutableMap<SsaVariableName, AccessDependencies> = mutableMapOf()

    // Produce new ssa names for a source variable name
    private val ssaNameProducers: MutableMap<SymbolicName, FreshEntityProducer<SsaVariableName, SymbolicName>> =
        mutableMapOf()

    fun branch(
        condition: Exp,
        thenBlock: () -> Unit,
        elseBlock: () -> Unit
    ) {
        val splitPoint = head
        head = splitPoint.generateBranchingBlockNodeFromThisNode(condition)
        thenBlock()
        val thenResultHead = head
        head = splitPoint.generateBranchingBlockNodeFromThisNode(Exp.Not(condition))
        elseBlock()
        val joinNode = SsaJoinNode(
            thenResultHead,
            head,
            condition,
            this
        )
        head = SsaBlockNode(joinNode, splitPoint.fullBranchingCondition)
    }

    fun constructExpression(): Exp {
        if (returnExpressions.isEmpty()) throw SnaktInternalException(
            source,
            "No return expression was found for translation"
        )
        val defaultBody = returnExpressions.last().second
        val bodyExp = returnExpressions.dropLast(1).foldRight(defaultBody) { expPair, elseBranch ->
            Exp.TernaryExp(
                expPair.first,
                expPair.second,
                elseBranch
            )
        }
        return ssaAssignments.foldRight(bodyExp) { assignment, innerScope ->
            Exp.LetBinding(Declaration.LocalVarDecl(assignment.first, Type.Ref), assignment.second, innerScope)
        }
    }

    fun generateFreshSsaName(name: SymbolicName): SsaVariableName {
        val producer = ssaNameProducers.getOrPut(name) { FreshEntityProducer(::SsaVariableName) }
        return producer.getFresh(name)
    }

    fun addAssignment(
        name: SymbolicName,
        varExp: Exp,
        newVarAccessDependencies: List<Exp.PredicateAccess> = emptyList()
    ) {
        val ssaName = head.updateLatestName(name)
        val dependencies = varExp.accessDependencies().combine(mapOf(Exp.BoolLit(true) to newVarAccessDependencies))
        accessDependencies[ssaName] = dependencies
        addGuardedAssignment(ssaName, varExp.withAccessDependencies(dependencies))
    }

    fun addPhiAssignment(condition: Exp, left: SsaVariableName, right: SsaVariableName, name: SsaVariableName) {
        if (left.baseName != right.baseName) {
            throw SnaktInternalException(
                source,
                "Phi Assignments may only be created for SSA variables referring to the same source variable."
            )
        }
        val phiExpression = Exp.TernaryExp(
            condition,
            Exp.LocalVar(left, Type.Ref),
            Exp.LocalVar(right, Type.Ref)
        )
        val dependencies = phiExpression.accessDependencies()
        accessDependencies[name] = dependencies
        addGuardedAssignment(name, phiExpression.withAccessDependencies(dependencies))
    }

    fun addReturn(returnExp: Exp) {
        returnExpressions.add(head.fullBranchingCondition to returnExp.withAccessDependencies(returnExp.accessDependencies()))
    }

    fun resolveVariableName(name: SymbolicName): SymbolicName {
        return head.resolveVariableName(name)
    }

    private fun addGuardedAssignment(name: SsaVariableName, varExp: Exp) {
        val defaultExpression = varExp.type.defaultExpression() ?: throw SnaktInternalException(
            source,
            "Tried to assign a variable without a default expression"
        )
        if (head.fullBranchingCondition == Exp.BoolLit(true)) {
            ssaAssignments.add(name to varExp)
        } else {
            ssaAssignments.add(name to Exp.TernaryExp(head.fullBranchingCondition, varExp, defaultExpression))
        }
    }

    private fun Exp.withAccessDependencies(dependencies: AccessDependencies): Exp {
        if (this !is Exp.FieldAccess && this !is Exp.FuncApp && this !is Exp.DomainFuncApp) return this
        val cases = dependencies.entries.groupBy({ it.value }, { it.key })
        // Each case unfolds its predicates around a copy of this expression; the last one needs no guard.
        val guarded = cases.map { (predicates, conditions) -> conditions.toDisjunction() to predicates.asUnfoldingIn(this) }
        return guarded.dropLast(1).foldRight(guarded.last().second) { (condition, exp), elseExp ->
            Exp.TernaryExp(condition, exp, elseExp)
        }
    }

    private fun Exp.accessDependencies(): AccessDependencies =
        when (this) {
            is Exp.LocalVar -> accessDependencies[name] ?: noAccessDependencies
            is Exp.FieldAccess -> {
                if (rcv !is Exp.LocalVar) {
                    throw SnaktInternalException(source, "Access sources must be local variables, but received $rcv")
                }
                rcv.accessDependencies()
            }

            is Exp.FuncApp -> args.combinedAccessDependencies()
            is Exp.DomainFuncApp -> args.combinedAccessDependencies()
            is Exp.TernaryExp ->
                thenExp.accessDependencies().mapKeys { (condition, _) -> conjunction(condition, condExp) } +
                    elseExp.accessDependencies().mapKeys { (condition, _) -> conjunction(condition, Exp.Not(condExp)) }

            else -> noAccessDependencies
        }

    private fun List<Exp>.combinedAccessDependencies(): AccessDependencies =
        map { it.accessDependencies() }.distinct().fold(noAccessDependencies) { acc, dependencies -> acc.combine(dependencies) }

    /** Dependencies of using two values together: every pair of their cases can occur. */
    private fun AccessDependencies.combine(other: AccessDependencies): AccessDependencies =
        entries.flatMap { (condition, predicates) ->
            other.map { (otherCondition, otherPredicates) ->
                conjunction(condition, otherCondition) to (predicates + otherPredicates).distinct()
            }
        }.toMap()

    private fun conjunction(left: Exp, right: Exp): Exp =
        when {
            left == Exp.BoolLit(true) -> right
            right == Exp.BoolLit(true) -> left
            else -> Exp.And(left, right)
        }

    private fun List<Exp>.toDisjunction(): Exp = reduce { left, right -> Exp.Or(left, right) }

    private fun List<Exp.PredicateAccess>.asUnfoldingIn(exp: Exp): Exp =
        foldRight(exp) { access, acc -> Exp.Unfolding(access, acc) }
}

private typealias AccessDependencies = Map<Exp, List<Exp.PredicateAccess>>

private val noAccessDependencies: AccessDependencies = mapOf(Exp.BoolLit(true) to emptyList())
