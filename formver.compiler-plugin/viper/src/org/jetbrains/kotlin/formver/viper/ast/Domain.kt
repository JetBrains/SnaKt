/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*
import viper.silver.ast.AnonymousDomainAxiom
import viper.silver.ast.NamedDomainAxiom


data class DomainFunc(
    val name: SymbolicName,
    val domainName: SymbolicName,
    val formalArgs: List<Declaration.LocalVarDecl>,
    val typeArgs: List<Type.TypeVar>,
    val returnType: Type,
    val unique: Boolean,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.DomainFunc>, Applicable {
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.DomainFunc =
        viper.silver.ast.DomainFunc(
            name.mangled,
            formalArgs.map { it.toSilver() }.toScalaSeq(),
            returnType.toSilver(),
            unique,
            null.toScalaOption(),
            pos.toSilver(),
            info.toSilver(),
            domainName.mangled,
            trafos.toSilver()
        )

    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp.DomainFuncApp =
        Exp.DomainFuncApp(this, args, typeArgs.associateWith { it }, pos, info)
}

class DomainAxiom(
    val name: SymbolicName?,
    val domainName: SymbolicName,
    val exp: Exp,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.DomainAxiom> {
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.DomainAxiom =
        when (name) {
            null -> AnonymousDomainAxiom(
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                domainName.mangled,
                trafos.toSilver()
            )

            else -> NamedDomainAxiom(
                name.mangled,
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                domainName.mangled,
                trafos.toSilver()
            )
        }
}

abstract class Domain(
    val name: SymbolicName,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.Domain> {


    open val includeInShortDump: Boolean = true
    abstract val typeVars: List<Type.TypeVar>
    abstract val functions: List<DomainFunc>
    abstract val axioms: List<DomainAxiom>
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.Domain =
        viper.silver.ast.Domain(
            name.mangled,
            functions.toSilver().toScalaSeq(),
            axioms.toSilver().toScalaSeq(),
            // Can't use List.toViper directly here as the type would end up being `List<Type>` instead of `List<TypeVar`.
            typeVars.map { it.toSilver() }.toScalaSeq(),
            null.toScalaOption(),
            pos.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )

    fun funcApp(
        func: DomainFunc,
        args: List<Exp>,
        typeVarMap: Map<Type.TypeVar, Type> = typeVars.associateWith { it },
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.DomainFuncApp = Exp.DomainFuncApp(func, args, typeVarMap, pos, info, trafos)
}

abstract class BuiltinDomain(
    baseName: SymbolicName,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) : Domain(baseName, pos, info, trafos) {
    override val includeInShortDump: Boolean = false
}
