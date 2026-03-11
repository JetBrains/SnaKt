/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*
import viper.silver.ast.*

/**
 * Marker interface for binary Viper expressions that have a [left] and [right] operand.
 *
 * Shared by all arithmetic, comparison, Boolean, and sequence binary operations so that
 * generic traversal code (e.g. [registerExpNames] in `Program.kt`) can recurse into both
 * sub-expressions without pattern-matching every individual node.
 */
sealed interface BinaryExp : Exp {
    val left: Exp
    val right: Exp
}

/**
 * Sealed hierarchy of Viper expressions.
 *
 * Each nested class represents one node in the Viper expression language and implements
 * [IntoSilver] to produce the matching `viper.silver.ast` node for Silicon.
 *
 * Every expression carries a [type] (a [Type] instance) and the standard Silver AST metadata
 * (`pos`, `info`, `trafos`) that are left at their defaults in normal SnaKt usage.
 *
 * Kotlin-friendly operator overloads (`+`, `-`, `*`, `/`, `%`, `!`, infix `and`/`or`/`eq`/…)
 * are defined as extension functions at the bottom of the file for ergonomic expression
 * construction.
 */
sealed interface Exp : IntoSilver<viper.silver.ast.Exp> {

    val type: Type

    /** Arithmetic negation of [arg]; always has type [Type.Int]. */
    data class Minus(
        val arg: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Minus =
            Minus(arg.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    //region Arithmetic Expressions
    /** Integer addition: `left + right`. */
    data class Add(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Add =
            Add(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** Integer subtraction: `left - right`. */
    data class Sub(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Sub =
            Sub(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** Integer multiplication: `left * right`. */
    data class Mul(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Mul =
            Mul(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** Integer division: `left \ right` (truncated towards negative infinity in Viper). */
    data class Div(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Div =
            Div(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** Integer modulo: `left % right`. */
    data class Mod(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Mod =
            Mod(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }
    //endregion

    //region Integer Comparison Expressions
    /** Integer less-than comparison: `left < right`. Result type is [Type.Bool]. */
    data class LtCmp(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.LtCmp =
            LtCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** Integer less-than-or-equal comparison: `left <= right`. Result type is [Type.Bool]. */
    data class LeCmp(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.LeCmp =
            LeCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** Integer greater-than comparison: `left > right`. Result type is [Type.Bool]. */
    data class GtCmp(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.GtCmp =
            GtCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** Integer greater-than-or-equal comparison: `left >= right`. Result type is [Type.Bool]. */
    data class GeCmp(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.GeCmp =
            GeCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }
    //endregion

    //region Boolean Comparison Expressions
    /** Structural (value) equality: `left == right`. Works on any [Type]; result is [Type.Bool]. */
    data class EqCmp(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.EqCmp =
            EqCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** Structural (value) inequality: `left != right`. Works on any [Type]; result is [Type.Bool]. */
    data class NeCmp(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.NeCmp =
            NeCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }
    //endregion

    //region Boolean Expressions
    /** Short-circuit logical conjunction: `left && right`. */
    data class And(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.And =
            And(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** Short-circuit logical disjunction: `left || right`. */
    data class Or(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Or =
            Or(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** Logical implication: `left ==> right`. */
    data class Implies(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Implies =
            Implies(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** Logical negation of [arg]. */
    data class Not(
        val arg: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Not =
            Not(arg.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }
    //endregion

    //region Quantifier Expressions
    /**
     * A quantifier trigger (also called a *pattern*) that guides the SMT solver's
     * instantiation strategy for the enclosing [Forall] or [Exists].
     *
     * A trigger is a list of expressions; the solver instantiates the quantifier whenever
     * all expressions in the list match terms already present in the proof context.
     */
    class Trigger(
        val exps: List<Exp>,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : IntoSilver<viper.silver.ast.Trigger> {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Trigger =
            Trigger(exps.toSilver().toScalaSeq(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /**
     * Universal quantification over [variables] with optional [triggers] and body [exp].
     *
     * Always has type [Type.Bool].  Prefer constructing via [Exp.forall] helper functions
     * which pass the bound variables as [LocalVar] expressions into the builder lambda.
     */
    data class Forall(
        val variables: List<Declaration.LocalVarDecl>,
        val triggers: List<Trigger>,
        val exp: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Forall =
            Forall(
                variables.map { it.toSilver() }.toScalaSeq(),
                triggers.toSilver().toScalaSeq(),
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver()
            )
    }

    /**
     * Existential quantification over [variables] with optional [triggers] and body [exp].
     *
     * Always has type [Type.Bool].
     */
    data class Exists(
        val variables: List<Declaration.LocalVarDecl>,
        val triggers: List<Trigger>,
        val exp: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Exists =
            Exists(
                variables.map { it.toSilver() }.toScalaSeq(),
                triggers.toSilver().toScalaSeq(),
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver()
            )
    }

    //region Literals
    /** A Viper integer literal. The Kotlin [Int] value is converted to a Scala `BigInt` by [toSilver]. */
    data class IntLit(
        val value: Int,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Int
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.IntLit =
            IntLit(value.toScalaBigInt(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** The Viper `null` reference literal; always has type [Type.Ref]. */
    data class NullLit(
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Ref
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.NullLit = NullLit(pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /** A Boolean literal (`true` or `false`). */
    data class BoolLit(
        val value: Boolean,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.BoolLit =
            viper.silver.ast.BoolLit.apply(value, pos.toSilver(), info.toSilver(), trafos.toSilver())
    }
    //endregion

    /**
     * A reference to a local variable with a symbolic [name] and a static [type].
     *
     * [name] is mangled to a globally unique Silver identifier during [toSilver] conversion.
     */
    data class LocalVar(
        val name: SymbolicName,
        override val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.LocalVar =
            LocalVar(name.mangled, type.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /**
     * Heap field read: `rcv.field`.
     *
     * The type is derived from the field's declared type.  Reading a field requires the caller
     * to hold at least read permission to `acc(rcv.field)`.
     */
    data class FieldAccess(
        val rcv: Exp,
        val field: Field,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = field.type
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.FieldAccess =
            FieldAccess(rcv.toSilver(), field.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /**
     * The special `result` expression used inside Viper function postconditions to refer to
     * the function's return value.
     */
    data class Result(
        override val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Result =
            Result(type.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /**
     * An application of a named Viper (pure) function to [args].
     *
     * The return [type] must be supplied explicitly because no function table is available at
     * AST construction time.
     */
    data class FuncApp(
        val functionName: SymbolicName,
        val args: List<Exp>,
        override val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.FuncApp = FuncApp(
            functionName.mangled,
            args.toSilver().toScalaSeq(),
            pos.toSilver(),
            info.toSilver(),
            type.toSilver(),
            trafos.toSilver()
        )
    }

    /**
     * IMPORTANT: typeVarMap needs to be set even when the type variables are
     * not instantiated. In that case map the generic type variables to themselves.
     * Example: x is of type T, f(x: T) -> Int is a domain function, and you want to
     * make the generic domain function call f(x) then a Map from T -> T needs to be
     * supplied.
     */
    data class DomainFuncApp(
        val function: DomainFunc,
        val args: List<Exp>,
        val typeVarMap: Map<Type.TypeVar, Type>,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        context(nameResolver: NameResolver)
        private val scalaTypeVarMap: scala.collection.immutable.Map<TypeVar, viper.silver.ast.Type>
            get() = typeVarMap.mapKeys { it.key.toSilver() }.mapValues { it.value.toSilver() }.toScalaMap()
        override val type = function.returnType.substitute(typeVarMap)
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Exp =
            viper.silver.ast.DomainFuncApp.apply(
                function.toSilver(),
                args.toSilver().toScalaSeq(),
                scalaTypeVarMap,
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver()
            )
    }

    /**
     * A Viper sequence literal built from an explicit list of elements: `Seq(e1, e2, …)`.
     *
     * The element type is inferred from the first element; all elements must share the same type.
     */
    data class ExplicitSeq(
        val args: List<Exp>,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.ExplicitSeq =
            ExplicitSeq(
                args.toSilver().toScalaSeq(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver(),
            )

        override val type = Type.Seq(args.first().type)
    }

    /** An empty Viper sequence with element type [elementType]: `Seq[T]()`. */
    data class EmptySeq(
        val elementType: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.EmptySeq =
            viper.silver.ast.EmptySeq.apply(
                elementType.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver(),
            )

        override val type = Type.Seq(elementType)
    }

    /** Length of sequence [seq]: `|seq|`. Result type is [Type.Int]. */
    data class SeqLength(
        val seq: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.SeqLength =
            viper.silver.ast.SeqLength.apply(
                seq.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver(),
            )

        override val type = Type.Int
    }

    /** Prefix of [seq] up to (but not including) index [idx]: `seq[..idx]`. */
    data class SeqTake(
        val seq: Exp,
        val idx: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.SeqTake =
            viper.silver.ast.SeqTake.apply(
                seq.toSilver(),
                idx.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver(),
            )

        override val type = seq.type
    }

    /** Element of [seq] at zero-based index [idx]: `seq[idx]`. Result type is [Type.Int]. */
    data class SeqIndex(
        val seq: Exp,
        val idx: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.SeqIndex =
            viper.silver.ast.SeqIndex.apply(
                seq.toSilver(),
                idx.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver(),
            )

        override val type = Type.Int
    }

    /** Sequence concatenation: `left ++ right`. Result type matches [left]'s type. */
    data class SeqAppend(
        override val left: Exp,
        override val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : BinaryExp {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.SeqAppend =
            viper.silver.ast.SeqAppend.apply(
                left.toSilver(),
                right.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver(),
            )

        override val type = left.type
    }

    /**
     * Evaluates [exp] in the heap state at method entry (the `old` heap).
     *
     * Used in postconditions to refer to pre-call values, e.g. `old(x.f)`.
     */
    data class Old(
        val exp: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = exp.type
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Old =
            Old(exp.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /**
     * An access predicate `acc(P(args), perm)` representing a fractional permission to the
     * predicate named [predicateName] applied to [formalArgs] with amount [perm].
     *
     * [toSilver] always produces a `PredicateAccessPredicate` rather than a bare `PredicateAccess`
     * because the latter does not work reliably with Silicon when used outside of `acc(…)` context.
     * The Silver `type` is set to [Type.Bool] for consistency with the Silver type system.
     */
    data class PredicateAccess(
        val predicateName: SymbolicName,
        val formalArgs: List<Exp>,
        val perm: PermExp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        // The type is set to Bool just to be consistent with Silver type. Probably it will never be used
        override val type: Type = Type.Bool

        // Note: since the simple syntax P(...) has the same meaning as acc(P(...)), which in turn has the same meaning as acc(P(...), write)
        // It is always better to deal with PredicateAccessPredicate because PredicateAccess seems not working well with Silver
        context(nameResolver: NameResolver)
        override fun toSilver(): PredicateAccessPredicate {
            val predicateAccess = PredicateAccess(
                formalArgs.toSilver().toScalaSeq(),
                predicateName.mangled,
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver()
            )
            return PredicateAccessPredicate(
                predicateAccess,
                perm.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver()
            )
        }
    }

    /**
     * Temporarily unfolds [predicateAccess] to evaluate [body], then refolds it.
     *
     * Used in pure contexts (Viper functions and specifications) where a `fold`/`unfold`
     * statement pair is not available.
     */
    data class Unfolding(
        val predicateAccess: PredicateAccess,
        val body: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = body.type
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Unfolding =
            Unfolding(predicateAccess.toSilver(), body.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /**
     * A field access predicate `acc(rcv.field, perm)` asserting that the caller holds
     * fractional permission [perm] to the heap location described by [field].
     *
     * Serialised as a Silver `FieldAccessPredicate`.  Type is [Type.Bool].
     */
    data class Acc(
        val field: FieldAccess,
        val perm: PermExp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool

        context(nameResolver: NameResolver)
        override fun toSilver() = FieldAccessPredicate(
                field.toSilver(),
                perm.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver(),
            )
    }

    /**
     * A `let … in …` binding that binds [varExp] to [variable] within [body].
     *
     * Corresponds to Silver's `let x == (expr) in body` construct.  The type of the whole
     * expression equals the type of [variable].
     */
    data class LetBinding(
        val variable: Declaration.LocalVarDecl,
        val varExp: Exp,
        val body: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ): Exp {
        override val type: Type = variable.type

        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Let =
            Let(variable.toSilver(), varExp.toSilver(), body.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    /**
     * A conditional (ternary) expression: `condExp ? thenExp : elseExp`.
     *
     * Corresponds to Silver's `CondExp`.  The [thenExp] and [elseExp] branches must have the
     * same type; that type becomes the type of the entire expression (asserted at construction time).
     */
    data class TernaryExp(
        val condExp: Exp,
        val thenExp: Exp,
        val elseExp: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ): Exp {
        override val type: Type = thenExp.type.also { assert(it == elseExp.type) }

        context(nameResolver: NameResolver)
        override fun toSilver(): CondExp =
            CondExp(condExp.toSilver(), thenExp.toSilver(), elseExp.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    // We can't pass all the available position, info, and trafos information here.
    // Living with that seems fine for the moment.
    /** Convenience shorthand for constructing a [FieldAccess] expression for `this.field`. */
    fun fieldAccess(
        field: Field,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): FieldAccess =
        FieldAccess(this, field, pos, info, trafos)

    // We can't pass all the available position, info, and trafos information here.
    // Living with that seems fine for the moment.
    /** Convenience shorthand for constructing an [AccessPredicate.FieldAccessPredicate] for `acc(this.field, permission)`. */
    fun fieldAccessPredicate(
        field: Field,
        permission: PermExp,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): AccessPredicate.FieldAccessPredicate =
        AccessPredicate.FieldAccessPredicate(fieldAccess(field, pos), permission, pos, info, trafos)

    companion object {
        private fun forallImpl(vars: List<Declaration.LocalVarDecl>, action: ForallBuilder.() -> Exp): Exp {
            val builder = ForallBuilder(vars)
            val body = builder.action()
            return builder.toForallExp(body)
        }

        /**
         * Create an Exp.Forall node, passing the local variables as local variable access expressions into the action.
         */
        fun forall(v1: Var, action: ForallBuilder.(LocalVar) -> Exp): Exp =
            forallImpl(listOf(v1.decl())) { action(v1.use()) }

        fun forall(v1: Var, v2: Var, action: ForallBuilder.(LocalVar, LocalVar) -> Exp): Exp =
            forallImpl(listOf(v1.decl(), v2.decl())) { action(v1.use(), v2.use()) }

        fun forall(v1: Var, v2: Var, v3: Var, action: ForallBuilder.(LocalVar, LocalVar, LocalVar) -> Exp): Exp =
            forallImpl(listOf(v1.decl(), v2.decl(), v3.decl())) { action(v1.use(), v2.use(), v3.use()) }

        /**
         * Take the conjunction of the given expressions.
         */
        fun List<Exp>.toConjunction(): Exp =
            if (isEmpty()) BoolLit(true)
            else reduce { l, r -> And(l, r) }
    }

    /**
     * Builder for statements of the form
     * ```
     * forall vars :: { triggers } assumptions ==> conclusion
     * ```
     *
     * The assumptions and conclusion together are the body of the forall.
     *
     * This class is intended to be used via the `Exp.forall` functions, not directly.
     */
    class ForallBuilder(private val vars: List<Declaration.LocalVarDecl>) {
        private val triggers = mutableListOf<Trigger>()
        private val assumptions = mutableListOf<Exp>()

        fun toForallExp(conclusion: Exp): Exp {
            val body =
                if (assumptions.isNotEmpty()) {
                    Implies(assumptions.toConjunction(), conclusion)
                } else {
                    conclusion
                }
            return Forall(vars, triggers, body)
        }

        /**
         * Add an assumption to this forall statement.
         */
        fun assumption(action: () -> Exp): Exp {
            val exp = action()
            assumptions.add(exp)
            return exp
        }

        /**
         * Create a trigger consisting of a single expression.
         */
        fun simpleTrigger(action: () -> Exp): Exp {
            val exp = action()
            triggers.add(Trigger(listOf(exp)))
            return exp
        }

        /**
         * Create a trigger consisting of multiple expressions, and return them as a conjunction.
         *
         * Note that a compound trigger must contain at least one expression; an empty list does
         * not make sense here.
         */
        fun compoundTrigger(action: TriggerBuilder.() -> Unit): Exp {
            val builder = TriggerBuilder()
            builder.action()
            triggers.add(builder.toTrigger())
            return builder.toConjunction()
        }

        class TriggerBuilder {
            private val exps = mutableListOf<Exp>()

            /**
             * Add an expression to the trigger.
             */
            fun subTrigger(action: () -> Exp): Exp {
                val exp = action()
                exps.add(exp)
                return exp
            }

            fun toTrigger(): Trigger {
                assert(exps.isNotEmpty()) { "There is no point to having an empty trigger expression. " }
                return Trigger(exps)
            }

            fun toConjunction(): Exp = exps.toConjunction()
        }
    }
}

operator fun Exp.not() = Exp.Not(this)
infix fun Exp.or(other: Exp) = Exp.Or(this, other)
infix fun Exp.and(other: Exp) = Exp.And(this, other)
infix fun Exp.eq(other: Exp) = Exp.EqCmp(this, other)
infix fun Exp.ne(other: Exp) = Exp.NeCmp(this, other)
infix fun Exp.ge(other: Exp) = Exp.GeCmp(this, other)
infix fun Exp.le(other: Exp) = Exp.LeCmp(this, other)
infix fun Exp.gt(other: Exp) = Exp.GtCmp(this, other)
infix fun Exp.lt(other: Exp) = Exp.LtCmp(this, other)
infix operator fun Exp.plus(other: Exp) = Exp.Add(this, other)
infix operator fun Exp.minus(other: Exp) = Exp.Sub(this, other)
infix operator fun Exp.times(other: Exp) = Exp.Mul(this, other)
infix operator fun Exp.div(other: Exp) = Exp.Div(this, other)
infix operator fun Exp.rem(other: Exp) = Exp.Mod(this, other)
infix fun Exp.implies(other: Exp) = Exp.Implies(this, other)
fun Int.toExp() = Exp.IntLit(this)
fun Boolean.toExp() = Exp.BoolLit(this)

/**
 * Converts a Kotlin primitive value to the corresponding Viper literal expression.
 *
 * Supported types:
 * - `null` → [Exp.NullLit]
 * - [Int] → [Exp.IntLit]
 * - [Boolean] → [Exp.BoolLit]
 * - [Char] → [Exp.IntLit] (using the character code point)
 * - Empty [String] → [Exp.EmptySeq] of `Int`
 * - Non-empty [String] → [Exp.ExplicitSeq] of character code-point [Exp.IntLit] nodes
 *
 * Throws [IllegalStateException] for any other type.
 */
fun Any?.viperLiteral(
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
): Exp = when (this) {
    null -> Exp.NullLit(pos, info, trafos)
    is Int -> Exp.IntLit(this, pos, info, trafos)
    is Boolean -> Exp.BoolLit(this, pos, info, trafos)
    is Char -> Exp.IntLit(this.code, pos, info, trafos)
    is String ->
        if (isEmpty()) Exp.EmptySeq(Type.Int, pos, info, trafos)
        else Exp.ExplicitSeq(map { it.viperLiteral(pos, info, trafos) }, pos, info, trafos)

    else -> error("Literal type not known.")
}
