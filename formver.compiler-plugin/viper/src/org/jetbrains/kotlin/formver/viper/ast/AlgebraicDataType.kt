package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoSilver
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.emptyScalaMap
import org.jetbrains.kotlin.formver.viper.emptySeq
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.formver.viper.toScalaSeq
import org.jetbrains.kotlin.formver.viper.toSilver
import viper.silver.plugin.standard.adt.Adt
import viper.silver.plugin.standard.adt.AdtConstructor
import viper.silver.plugin.standard.adt.AdtType

/**
 * Represents an ADT constructor on the Viper level.
 *
 * It is used during the construction of the ADT type and also called during the construction of a new instance,
 * linking the type and its usage.
 */
data class AdtConstructor(
    val name: SymbolicName,
    val adtName: SymbolicName,
    val formalArgs: List<Declaration.LocalVarDecl>,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<AdtConstructor> {
    context(nameResolver: NameResolver)
    override fun toSilver(): AdtConstructor = AdtConstructor(
            name.mangled,
            formalArgs.map { it.toSilver() }.toScalaSeq(),
            pos.toSilver(),
            info.toSilver(),
            AdtType(adtName.mangled, emptyScalaMap(), emptySeq()),
            adtName.mangled,
            trafos.toSilver(),
        )
}

/** A Viper ADT declaration with a name, a list of constructors, and optional type parameters. */
data class AlgebraicDataType(
    val name: SymbolicName,
    val constructors: List<org.jetbrains.kotlin.formver.viper.ast.AdtConstructor>,
    val typeVars: List<Type.TypeVar> = emptyList(),
    val includeInShortDump: Boolean = true,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<Adt> {
    context(nameResolver: NameResolver)
    override fun toSilver(): Adt =
        Adt(
            name.mangled,
            constructors.toSilver().toScalaSeq(),
            typeVars.map { it.toSilver() }.toScalaSeq(),
            emptyScalaMap(),
            pos.toSilver(),
            info.toSilver(),
            trafos.toSilver(),
        )
}