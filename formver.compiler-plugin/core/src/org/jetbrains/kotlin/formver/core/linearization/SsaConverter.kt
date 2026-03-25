package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction
import org.jetbrains.kotlin.formver.viper.ast.Type

class SsaConverter(
    val source: KtSourceElement? = null,
) {
    private var head: SsaBlockNode = SsaBlockNode(SsaStartNode(), Exp.BoolLit(true))
    private val ssaAssignments: MutableList<Pair<SsaVariableName, Exp>> = mutableListOf()
    private val returnExpressions: MutableList<Pair<Exp, Exp>> = mutableListOf()

    // An entry in this map means that to accessing the let-bound variable SSAVariableName under condition Exp requires predicates in the List<Exp.PredicateAccess> unfolded
    private val accessDependencies: MutableMap<SsaVariableName, Map<Exp, List<Exp.PredicateAccess>>> = mutableMapOf()

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
        varExp.propagateAccessDependencies(ssaName)
        val propagatedDependencies = accessDependencies[ssaName]
        if (propagatedDependencies == null || propagatedDependencies.isEmpty()) {
            accessDependencies[ssaName] = mapOf(Exp.BoolLit(true) to newVarAccessDependencies)
        } else if (newVarAccessDependencies.isNotEmpty()) {
            val withNewDependencies =
                propagatedDependencies.mapValues { (_, value) -> value + newVarAccessDependencies }
            accessDependencies[ssaName] = withNewDependencies
        }
        addGuardedAssignment(ssaName, varExp.withAccessDependencies(ssaName))
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
        phiExpression.propagateAccessDependencies(name)
        addGuardedAssignment(name, phiExpression.withAccessDependencies(name))
    }

    fun addReturn(returnExp: Exp) {
        returnExpressions.add(head.fullBranchingCondition to returnExp)
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

    private fun Exp.withAccessDependencies(name: SsaVariableName): Exp =
        when (this) {
            is Exp.FieldAccess, is Exp.FuncApp, is Exp.DomainFuncApp -> {
                val accesses = accessDependencies[name]
                when {
                    accesses == null || accesses.isEmpty() -> this
                    accesses.size == 1 -> {
                        val toUnfold = accesses.values.first()
                        toUnfold.asUnfoldingIn(this)
                    }

                    else -> {
                        val defaultExpression = this.type.defaultExpression() ?: throw SnaktInternalException(
                            source,
                            "Tried to assign a variable without a default expression"
                        )
                        val asExpressions = accesses.mapValues { (_, predicates) -> predicates.asUnfoldingIn(this) }
                        asExpressions.entries.fold(defaultExpression) { acc, entry ->
                            Exp.TernaryExp(entry.key, entry.value, acc)
                        }
                    }
                }
            }

            else -> this
        }

    private fun mergeAccessDependencies(from: SymbolicName, newName: SsaVariableName) {
        val fromDependencies = accessDependencies[from] ?: emptyMap()
        val toDependencies = accessDependencies[newName] ?: emptyMap()
        accessDependencies[newName] = fromDependencies.mergeWith(toDependencies)
    }

    private fun Map<Exp, List<Exp.PredicateAccess>>.mergeWith(
        other: Map<Exp, List<Exp.PredicateAccess>>
    ): Map<Exp, List<Exp.PredicateAccess>> {
        return (this.keys + other.keys).associateWith { key ->
            ((this[key] ?: emptyList()) + (other[key] ?: emptyList())).distinct()
        }
    }

    private fun Exp.propagateAccessDependencies(to: SsaVariableName) {
        val sourceExp = when (this) {
            is Exp.LocalVar -> this
            is Exp.FieldAccess -> this.rcv
            is Exp.FuncApp -> {
                this.args.forEach { it.propagateAccessDependencies(to) }
                return
            }

            is Exp.DomainFuncApp -> {
                this.args.forEach { it.propagateAccessDependencies(to) }
                return
            }

            is Exp.TernaryExp -> {
                val leftInvariants = accessDependencies[(this.thenExp as? Exp.LocalVar)?.name] ?: emptyMap()
                val rightInvariants = accessDependencies[(this.elseExp as? Exp.LocalVar)?.name] ?: emptyMap()
                val leftAmended = leftInvariants.mapKeys { (condition, _) ->
                    listOf(condition, this.condExp).toConjunction()
                }
                val rightAmended = rightInvariants.mapKeys { (condition, _) ->
                    listOf(condition, Exp.Not(this.condExp)).toConjunction()
                }
                accessDependencies[to] = leftAmended.mergeWith(rightAmended)
                return
            }

            else -> return
        }
        if (sourceExp !is Exp.LocalVar) {
            throw SnaktInternalException(source, "Access sources must be local variables, but received $sourceExp")
        }
        mergeAccessDependencies(sourceExp.name, to)
    }

    private fun List<Exp.PredicateAccess>.asUnfoldingIn(exp: Exp): Exp =
        foldRight(exp) { access, acc -> Exp.Unfolding(access, acc) }
}