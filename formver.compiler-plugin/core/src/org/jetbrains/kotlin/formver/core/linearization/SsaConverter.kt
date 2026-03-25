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
    private val accessInvariants: MutableMap<SsaVariableName, Map<Exp, List<Exp.PredicateAccess>>> = mutableMapOf()

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
        newVarAccessInvariants: List<Exp.PredicateAccess> = emptyList()
    ) {
        val ssaName = head.updateLatestName(name)
        varExp.propagateAccessInvariants(ssaName)
        if (newVarAccessInvariants.isNotEmpty()) {
            val propagatedInvariants = accessInvariants[ssaName]
            if (propagatedInvariants == null || propagatedInvariants.isEmpty()) {
                accessInvariants[ssaName] = mapOf(Exp.BoolLit(true) to newVarAccessInvariants)
            } else {
                val withNewInvariants = propagatedInvariants.mapValues { (_, value) -> value + newVarAccessInvariants }
                accessInvariants[ssaName] = withNewInvariants
            }
        }
        addGuardedAssignment(ssaName, varExp.withAccessInvariants(ssaName))
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
        phiExpression.propagateAccessInvariants(name)
        addGuardedAssignment(name, phiExpression.withAccessInvariants(name))
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

    private fun Exp.withAccessInvariants(name: SsaVariableName): Exp =
        when (this) {
            is Exp.FieldAccess, is Exp.FuncApp, is Exp.DomainFuncApp -> {
                val accesses = accessInvariants[name]
                when {
                    accesses == null || accesses.isEmpty() -> this
                    accesses.size == 1 -> {
                        val toUnfold = accesses.values.first()
                        if (toUnfold.isEmpty()) {
                            this
                        } else {
                            toUnfold.foldRight(this) { access, acc ->
                                Exp.Unfolding(access, acc)
                            }
                        }
                    }

                    else -> {
                        val asExpressions = accesses.mapValues { (_, predicates) ->
                            predicates.foldRight<Exp.PredicateAccess, Exp>(this) { access, acc ->
                                Exp.Unfolding(access, acc)
                            }
                        }
                        val defaultExpression = this.type.defaultExpression() ?: throw SnaktInternalException(
                            source,
                            "Tried to assign a variable without a default expression"
                        )
                        asExpressions.entries.fold(defaultExpression) { acc, entry ->
                            Exp.TernaryExp(entry.key, entry.value, acc)
                        }
                    }
                }
            }

            else -> this
        }

    private fun mergeAccessInvariants(from: SymbolicName, newName: SsaVariableName) {
        val fromInvariants = accessInvariants[from] ?: emptyMap()
        val toInvariants = accessInvariants[newName] ?: emptyMap()
        accessInvariants[newName] = fromInvariants.mergeWith(toInvariants)
    }

    private fun Map<Exp, List<Exp.PredicateAccess>>.mergeWith(
        other: Map<Exp, List<Exp.PredicateAccess>>
    ): Map<Exp, List<Exp.PredicateAccess>> {
        return (this.keys + other.keys).associateWith { key ->
            ((this[key] ?: emptyList()) + (other[key] ?: emptyList())).distinct()
        }
    }

    private fun Exp.propagateAccessInvariants(to: SsaVariableName) {
        val sourceExp = when (this) {
            is Exp.LocalVar -> this
            is Exp.FieldAccess -> this.rcv
            is Exp.FuncApp -> {
                this.args.forEach { it.propagateAccessInvariants(to) }
                return
            }

            is Exp.DomainFuncApp -> {
                this.args.forEach { it.propagateAccessInvariants(to) }
                return
            }

            is Exp.TernaryExp -> {
                val leftInvariants = accessInvariants[(this.thenExp as? Exp.LocalVar)?.name] ?: emptyMap()
                val rightInvariants = accessInvariants[(this.elseExp as? Exp.LocalVar)?.name] ?: emptyMap()
                val leftAmended = leftInvariants.mapKeys { (condition, _) ->
                    listOf(condition, this.condExp).toConjunction()
                }
                val rightAmended = rightInvariants.mapKeys { (condition, _) ->
                    listOf(condition, Exp.Not(this.condExp)).toConjunction()
                }
                accessInvariants[to] = leftAmended.mergeWith(rightAmended)
                return
            }

            else -> return
        }
        if (sourceExp !is Exp.LocalVar) {
            throw SnaktInternalException(source, "Access sources must be local variables $sourceExp")
        }
        mergeAccessInvariants(sourceExp.name, to)
    }
}