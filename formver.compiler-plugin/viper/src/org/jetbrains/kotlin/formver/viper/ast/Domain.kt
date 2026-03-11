/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*
import viper.silver.ast.AnonymousDomainAxiom
import viper.silver.ast.NamedDomainAxiom

/**
 * We also convert domain names and their function and axiom names as
 * they have to be globally unique as well.
 */

/**
 * The [SymbolicName] of a Viper domain declaration.
 *
 * Mangles with type prefix `"d"` so that domain names are globally unique and distinguishable
 * from other Silver identifiers.
 */
data class DomainName(val baseName: String) : SymbolicName {
    override val mangledType: String
        get() = "d"
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName
}
/**
 * A domain function name that is not prefixed with its owning domain name.
 *
 * Used as the `funcName` component of a [QualifiedDomainFuncName]; the base name is used
 * verbatim without a scope prefix during mangling.
 */
data class UnqualifiedDomainFuncName(val baseName: String) : SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName
}

/**
 * A fully qualified domain function name that includes the owning [domainName] as its scope.
 *
 * Mangles with type prefix `"df"` and uses [domainName]'s base name as the scope so that
 * domain function identifiers are globally unique across all domains.
 */
data class QualifiedDomainFuncName(val domainName: DomainName, val funcName: SymbolicName) : SymbolicName {
    override val mangledType: String
        get() = "df"
    context(nameResolver: NameResolver)
    override val mangledScope: String
        get() = domainName.mangledBaseName
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = funcName.mangled
}

/** Represents the name of a possible anonymous axiom.
 *
 * We need the domain name regardless because of how Viper is set up, hence the somewhat
 * unusual
 */
sealed interface OptionalDomainAxiomLabel {
    val domainName: DomainName
}

/**
 * An axiom label that carries both a [domainName] (required by Viper) and a human-readable
 * [baseName] that will be mangled into the Silver identifier.
 */
data class NamedDomainAxiomLabel(override val domainName: DomainName, val baseName: String) :
    OptionalDomainAxiomLabel, SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledScope: String
        get() = domainName.mangledBaseName

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName
}

/** An axiom with no name; only the owning [domainName] is required by the Silver AST. */
data class AnonymousDomainAxiomLabel(override val domainName: DomainName) : OptionalDomainAxiomLabel

/**
 * A function that lives inside a Viper domain.
 *
 * Domain functions are uninterpreted by default; their semantics are defined entirely by
 * the [DomainAxiom] entries of the enclosing [Domain].
 *
 * @property name        Fully qualified name, including the owning domain.
 * @property formalArgs  Formal parameter declarations.
 * @property typeArgs    Generic type parameters; must be non-empty for polymorphic domains.
 * @property returnType  Return type, which may reference entries in [typeArgs].
 * @property unique      When `true`, the function is injective (distinct inputs produce distinct outputs).
 */
data class DomainFunc(
    val name: QualifiedDomainFuncName,
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
            name.domainName.mangled,
            trafos.toSilver()
        )

    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp.DomainFuncApp =
        Exp.DomainFuncApp(this, args, typeArgs.associateWith { it }, pos, info)
}

/**
 * An axiom inside a Viper domain that constrains the interpretation of one or more
 * [DomainFunc] entries.
 *
 * The axiom body [exp] is a closed boolean formula (typically a universally quantified
 * implication).  If [name] is a [NamedDomainAxiomLabel] the axiom gets an identifier in
 * the Silver output; if it is an [AnonymousDomainAxiomLabel] no name is emitted.
 */
class DomainAxiom(
    val name: OptionalDomainAxiomLabel,
    val exp: Exp,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.DomainAxiom> {
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.DomainAxiom =
        when (name) {
            is NamedDomainAxiomLabel -> NamedDomainAxiom(
                name.mangled,
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                name.domainName.mangled,
                trafos.toSilver()
            )
            is AnonymousDomainAxiomLabel -> AnonymousDomainAxiom(
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                name.domainName.mangled,
                trafos.toSilver()
            )
        }
}

/**
 * Base class for a Viper domain: an abstract algebraic structure declared at the top level
 * of a [Program].
 *
 * Domains are the mechanism Viper provides for defining custom uninterpreted sorts together
 * with domain functions and axioms.  In SnaKt they are used to encode Kotlin's runtime type
 * hierarchy and to introduce type-tag and subtype-relation functions.
 *
 * Concrete subclasses must supply [typeVars], [functions], and [axioms].  Helper methods
 * ([createDomainFunc], [createNamedDomainAxiom], [createAnonymousDomainAxiom], [funcApp])
 * make it ergonomic to build these lists from within the subclass.
 *
 * @property name              The [DomainName] derived from [baseName].
 * @property includeInShortDump Whether this domain should appear in abbreviated Viper dump output.
 */
abstract class Domain(
    baseName: String,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.Domain> {
    val name = DomainName(baseName)

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

    // Don't use this directly, instead, use the custom types defined in `org.jetbrains.kotlin.formver.viper.ast.Type` for specific domains.
    /** Returns the [Type.Domain] that refers to this domain, applying optional type-parameter substitutions. */
    fun toType(typeParamSubst: Map<Type.TypeVar, Type> = typeVars.associateWith { it }): Type.Domain =
        Type.Domain(name, typeVars, typeParamSubst)

    /** Creates a [DomainFunc] qualified with this domain's name. */
    fun createDomainFunc(funcName: SymbolicName, args: List<Declaration.LocalVarDecl>, type: Type, unique: Boolean = false) =
        DomainFunc(QualifiedDomainFuncName(this.name, funcName), args, typeVars, type, unique)

    /** Creates a named [DomainAxiom] labelled [axiomName] with body [exp]. */
    fun createNamedDomainAxiom(axiomName: String, exp: Exp): DomainAxiom =
        DomainAxiom(NamedDomainAxiomLabel(this.name, axiomName), exp)

    /** Creates an anonymous [DomainAxiom] (no name in the Silver output) with body [exp]. */
    fun createAnonymousDomainAxiom(exp: Exp): DomainAxiom = DomainAxiom(AnonymousDomainAxiomLabel(this.name), exp)

    /**
     * Constructs an [Exp.DomainFuncApp] that applies domain function [func] to [args].
     *
     * The default [typeVarMap] maps each of this domain's [typeVars] to itself (identity
     * substitution), which is correct for non-instantiated generic calls.
     */
    fun funcApp(
        func: DomainFunc,
        args: List<Exp>,
        typeVarMap: Map<Type.TypeVar, Type> = typeVars.associateWith { it },
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.DomainFuncApp = Exp.DomainFuncApp(func, args, typeVarMap, pos, info, trafos)
}

/**
 * A [Domain] that is part of the internal Viper encoding infrastructure rather than
 * a user-visible Kotlin type.
 *
 * Built-in domains are excluded from the abbreviated Viper dump so that log output stays
 * focused on user-visible declarations.
 */
abstract class BuiltinDomain(
    baseName: String,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) : Domain(baseName, pos, info, trafos) {
    override val includeInShortDump: Boolean = false
}