package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.TernaryExp
import org.jetbrains.kotlin.formver.viper.ast.Type

/**
 * Builds a Viper SSA-form expression from a sequence of variable assignments and conditional returns.
 *
 * Used by [PureLinearizer] to convert `@Pure` function bodies into Viper `function` expressions.
 * Each write to a variable (`x = expr`) is recorded as an SSA assignment: the variable is renamed to
 * a fresh version (`x$1`, `x$2`, …) and the assignment is accumulated as a Viper `let`-binding.
 * After an `if`/`else`, [branch] merges the two SSA contexts with phi-assignments that select the
 * correct version using a ternary expression.
 *
 * Once all statements have been processed, [constructExpression] assembles the final Viper expression:
 * a sequence of nested `let`-bindings (SSA assignments) over a ternary-chain (conditional returns).
 *
 * @property source Kotlin source element used for error reporting.
 */
class SsaConverter(
    val source: KtSourceElement? = null,
) {
    private var head: SsaBlockNode = SsaBlockNode(SsaStartNode(), Exp.BoolLit(true))
    private val ssaAssignments: MutableList<Pair<SsaVariableName, Exp>> = mutableListOf()
    private val returnExpressions: MutableList<Pair<Exp, Exp>> = mutableListOf()

    // Produce new ssa names for a source variable name
    private val ssaNameProducers: MutableMap<SymbolicName, FreshEntityProducer<SsaVariableName, SymbolicName>> =
        mutableMapOf()

    /**
     * Evaluates [thenBlock] and [elseBlock] in separate SSA contexts (where [condition] is
     * respectively true and false), then merges them with phi-assignments for any variables written
     * in either branch. After this call, [head] points to the merged join node.
     */
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

    /**
     * Assembles the final SSA-form Viper expression.
     *
     * Builds a nested `let`-binding tree (one binding per recorded SSA assignment) over a
     * ternary chain that selects the appropriate return value based on which branch was taken.
     *
     * @throws [SnaktInternalException] if no return expression was recorded.
     */
    fun constructExpression(): Exp {
        if (returnExpressions.isEmpty()) throw SnaktInternalException(
            source,
            "No return expression was found for translation"
        )
        val defaultBody = returnExpressions.last().second
        val bodyExp = returnExpressions.dropLast(1).foldRight(defaultBody) { expPair, elseBranch ->
            TernaryExp(
                expPair.first,
                expPair.second,
                elseBranch
            )
        }
        return ssaAssignments.foldRight(bodyExp) { assignment, innerScope ->
            Exp.LetBinding(Declaration.LocalVarDecl(assignment.first, Type.Ref), assignment.second, innerScope)
        }
    }

    /** Generates a fresh SSA name for [name] by appending an incrementing index suffix (e.g., `x$1`, `x$2`). */
    fun generateFreshSsaName(name: SymbolicName): SsaVariableName {
        val producer = ssaNameProducers.getOrPut(name) { FreshEntityProducer(::SsaVariableName) }
        return producer.getFresh(name)
    }

    /**
     * Records an assignment of [varExp] to a fresh SSA version of [name].
     * Updates the current block node's latest name mapping for [name].
     */
    fun addAssignment(name: SymbolicName, varExp: Exp) {
        val ssaName = head.updateLatestName(name)
        ssaAssignments.add(ssaName to varExp)
    }

    /**
     * Records a phi-assignment for [name]: selects between [left] and [right] SSA versions
     * using [condition] (`condition ? left : right`).
     *
     * Both [left] and [right] must refer to the same source variable (same [SsaVariableName.baseName]).
     *
     * @throws [SnaktInternalException] if [left] and [right] have different base names.
     */
    fun addPhiAssignment(condition: Exp, left: SsaVariableName, right: SsaVariableName, name: SsaVariableName) {
        if (left.baseName != right.baseName) {
            throw SnaktInternalException(
                source,
                "Phi Assignments may only be created for SSA variables referring to the same source variable."
            )
        }
        ssaAssignments.add(
            name to TernaryExp(
                condition,
                Exp.LocalVar(left, Type.Ref),
                Exp.LocalVar(right, Type.Ref)
            )
        )
    }

    /**
     * Records a conditional return: when the current branch's [SsaBlockNode.fullBranchingCondition]
     * is true, the function returns [returnExp].
     */
    fun addReturn(returnExp: Exp) {
        returnExpressions.add(head.fullBranchingCondition to returnExp)
    }

    /** Resolves [name] to its current SSA version in the current block node. */
    fun resolveVariableName(name: SymbolicName): SymbolicName {
        return head.resolveVariableName(name)
    }
}