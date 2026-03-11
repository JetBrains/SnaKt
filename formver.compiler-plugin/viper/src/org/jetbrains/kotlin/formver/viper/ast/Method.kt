/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*

/**
 * Base class for a Viper method declaration.
 *
 * A Viper method is an impure procedure: it can modify the heap, consume and produce
 * permissions, and call other methods.  Unlike Viper functions, methods are not pure and
 * cannot appear in specifications.
 *
 * Subclasses must supply [formalArgs] and [formalReturns]; they may override [pres],
 * [posts], and [body] to provide preconditions, postconditions, and an implementation.
 *
 * @property name     Symbolic name used to produce a globally unique mangled Silver identifier.
 * @property pres     Precondition expressions that must hold on entry.
 * @property posts    Postcondition expressions that must hold on exit.
 * @property body     Optional method body; `null` means the method is abstract (specification only).
 * @property includeInShortDump Controls whether this method appears in abbreviated Viper dump output.
 */
abstract class Method(
    val name: SymbolicName,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.Method> {
    open val includeInShortDump: Boolean = true
    abstract val formalArgs: List<Declaration.LocalVarDecl>
    abstract val formalReturns: List<Declaration.LocalVarDecl>
    open val pres: List<Exp> = listOf()
    open val posts: List<Exp> = listOf()
    open val body: Stmt.Seqn? = null
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.Method =
        viper.silver.ast.Method(
            name.mangled,
            formalArgs.map { it.toSilver() }.toScalaSeq(),
            formalReturns.map { it.toSilver() }.toScalaSeq(),
            pres.toSilver().toScalaSeq(),
            posts.toSilver().toScalaSeq(),
            body.toScalaOption().map { it.toSilver() },
            pos.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )

    /**
     * Constructs a [Stmt.MethodCall] that invokes this method with the given [args] and
     * assigns results to [targets].
     */
    fun toMethodCall(
        args: List<Exp>,
        targets: List<Exp.LocalVar>,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ) = Stmt.MethodCall(name, args, targets, pos, info, trafos)
}

/**
 * A concrete Viper method produced from a user-written Kotlin function.
 *
 * Exactly one return variable ([returnVar]) is always present, matching SnaKt's convention
 * of wrapping single-return Kotlin functions.
 */
class UserMethod(
    name: SymbolicName,
    override val formalArgs: List<Declaration.LocalVarDecl>,
    returnVar: Declaration.LocalVarDecl,
    override val pres: List<Exp>,
    override val posts: List<Exp>,
    override val body: Stmt.Seqn?,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) : Method(name, pos, info, trafos) {
    override val formalReturns: List<Declaration.LocalVarDecl> = listOf(returnVar)
}

/**
 * Base class for internally generated helper methods (e.g. allocation stubs, runtime-type
 * helpers) that are part of the Viper encoding infrastructure rather than user code.
 *
 * Built-in methods are excluded from the abbreviated Viper dump so that log output
 * stays focused on user-visible methods.
 */
abstract class BuiltInMethod(
    name: SymbolicName,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) : Method(name, pos, info, trafos) {
    override val includeInShortDump: Boolean = false
}
