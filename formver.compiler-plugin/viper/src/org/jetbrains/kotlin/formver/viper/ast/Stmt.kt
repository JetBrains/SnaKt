/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*

/**
 * Sealed hierarchy of Viper statements.
 *
 * Each data class in this hierarchy corresponds to a Viper statement node and implements
 * [IntoSilver] to produce the matching `viper.silver.ast` node consumed by Silicon.
 *
 * The standard `pos`, `info`, and `trafos` parameters carried by every node are Silver AST
 * metadata (source position, annotations, transformation provenance) and are left at their
 * no-op defaults in normal SnaKt usage.
 *
 * Use [Stmt.assign] to create an assignment statement without having to inspect the type of
 * the left-hand side manually.
 */
sealed interface Stmt : IntoSilver<viper.silver.ast.Stmt> {

    /** Assigns the value of [rhs] to the local variable [lhs]. */
    data class LocalVarAssign(
        val lhs: Exp.LocalVar,
        val rhs: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.LocalVarAssign =
            viper.silver.ast.LocalVarAssign(
                lhs.toSilver(),
                rhs.toSilver(),
                position.toSilver(),
                info.toSilver(),
                trafos.toSilver()
            )
    }

    /** Assigns the value of [rhs] to the heap field [lhs]. */
    data class FieldAssign(
        val lhs: Exp.FieldAccess,
        val rhs: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.FieldAssign =
            viper.silver.ast.FieldAssign(
                lhs.toSilver(),
                rhs.toSilver(),
                position.toSilver(),
                info.toSilver(),
                trafos.toSilver()
            )
    }

    companion object {
        /**
         * Constructs the appropriate assignment statement based on the runtime type of [lhs].
         *
         * - [Exp.LocalVar] produces a [LocalVarAssign].
         * - [Exp.FieldAccess] produces a [FieldAssign].
         * - Any other expression throws [IllegalArgumentException] because it is not a valid lvalue.
         */
        fun assign(
            lhs: Exp,
            rhs: Exp,
            position: Position = Position.NoPosition,
            info: Info = Info.NoInfo,
            trafos: Trafos = Trafos.NoTrafos,
        ): Stmt = when (lhs) {
            is Exp.LocalVar ->
                LocalVarAssign(lhs, rhs, position, info, trafos)

            is Exp.FieldAccess ->
                FieldAssign(lhs, rhs, position, info, trafos)

            else -> {
                throw IllegalArgumentException("Expected an lvalue on the left-hand side of an assignment.")
            }
        }
    }

    /**
     * Invokes the Viper method identified by [methodName] with [args] and stores results in [targets].
     *
     * Corresponds to the Viper `methodName(args) returns (targets)` call syntax.
     */
    data class MethodCall(
        val methodName: SymbolicName,
        val args: List<Exp>,
        val targets: List<Exp.LocalVar>,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.MethodCall = viper.silver.ast.MethodCall(
            methodName.mangled,
            args.map { it.toSilver() }.toScalaSeq(),
            targets.map { it.toSilver() }.toScalaSeq(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    /**
     * Removes the permissions and pure facts described by [exp] from the current state.
     *
     * The verifier checks that the current state satisfies [exp] before removing it.
     */
    data class Exhale(
        val exp: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Exhale = viper.silver.ast.Exhale(
            exp.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    /**
     * Adds the permissions and pure facts described by [exp] to the current state without
     * checking that they already hold.
     *
     * Used to introduce assumptions, e.g. at the start of a method body to establish
     * precondition-derived permissions.
     */
    data class Inhale(
        val exp: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Inhale = viper.silver.ast.Inhale(
            exp.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    /**
     * Asserts that [exp] holds in the current state.
     *
     * Silicon verifies [exp] as a proof obligation; the program is rejected if the assertion
     * cannot be proved.
     */
    data class Assert(
        val exp: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Assert = viper.silver.ast.Assert(
            exp.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    /**
     * A sequential block of statements with locally scoped declarations.
     *
     * [scopedSeqnDeclarations] contains the [Declaration.LocalVarDecl] and [Declaration.LabelDecl]
     * entries that are in scope for the duration of this block, corresponding to the Silver `Seqn`
     * scoping rules.
     */
    data class Seqn(
        val stmts: List<Stmt> = listOf(),
        val scopedSeqnDeclarations: List<Declaration> = listOf(),
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Seqn = viper.silver.ast.Seqn(
            stmts.toSilver().toScalaSeq(),
            scopedSeqnDeclarations.toSilver().toScalaSeq(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    /**
     * A conditional statement branching on [cond].
     *
     * [els] is optional; when absent, [toSilver] supplies an empty [Seqn] to satisfy the
     * Silver AST requirement that both branches are always present.
     */
    data class If(
        val cond: Exp,
        val then: Seqn,
        val els: Seqn?,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.If = viper.silver.ast.If(
            cond.toSilver(),
            then.toSilver(),
            els?.toSilver() ?: Seqn().toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    /**
     * A while loop with a Boolean condition [cond], a list of loop [invariants], and a [body].
     *
     * Silicon verifies that each invariant holds on entry to the loop, is maintained by the
     * body, and holds again after the loop exits.
     */
    data class While(
        val cond: Exp,
        val invariants: List<Exp>,
        val body: Seqn,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.While = viper.silver.ast.While(
            cond.toSilver(),
            invariants.map { it.toSilver() }.toScalaSeq(),
            body.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    /**
     * A named program point, optionally annotated with [invariants] that must hold at this point.
     *
     * Labels are used as targets for [Goto] statements and for `old[label](exp)` heap-snapshot
     * expressions in Viper specifications.
     */
    data class Label(
        val name: SymbolicName,
        val invariants: List<Exp>,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Label = viper.silver.ast.Label(
            name.mangled,
            invariants.map { it.toSilver() }.toScalaSeq(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    /** Unconditional jump to the [Label] identified by [name]. */
    data class Goto(
        val name: SymbolicName,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Goto = viper.silver.ast.Goto(
            name.mangled,
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    /**
     * Folds the predicate instance [acc], consuming its body permissions and producing the
     * abstract predicate resource.
     */
    data class Fold(
        val acc: Exp.PredicateAccess,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Fold = viper.silver.ast.Fold(
            acc.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    /**
     * Unfolds the predicate instance [acc], consuming the abstract predicate resource and
     * producing its body permissions.
     */
    data class Unfold(
        val acc: Exp.PredicateAccess,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Unfold = viper.silver.ast.Unfold(
            acc.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }
}