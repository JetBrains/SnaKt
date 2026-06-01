/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*

abstract class Method(
    val name: SymbolicName,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
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
            silverNoTrafos
        )

    fun toMethodCall(
        args: List<Exp>,
        targets: List<Exp.LocalVar>,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
    ) = Stmt.MethodCall(name, args, targets, pos, info)
}

class UserMethod(
    name: SymbolicName,
    override val formalArgs: List<Declaration.LocalVarDecl>,
    returnVar: Declaration.LocalVarDecl,
    override val pres: List<Exp>,
    override val posts: List<Exp>,
    override val body: Stmt.Seqn?,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
) : Method(name, pos, info) {
    override val formalReturns: List<Declaration.LocalVarDecl> = listOf(returnVar)
}

class BuiltInMethod(
    name: SymbolicName,
    override val formalArgs: List<Declaration.LocalVarDecl>,
    returnVar: Declaration.LocalVarDecl,
    override val pres: List<Exp>,
    override val posts: List<Exp>,
    override val body: Stmt.Seqn?,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
) : Method(name, pos, info) {
    override val includeInShortDump: Boolean = false
    override val formalReturns: List<Declaration.LocalVarDecl> = listOf(returnVar)
}
