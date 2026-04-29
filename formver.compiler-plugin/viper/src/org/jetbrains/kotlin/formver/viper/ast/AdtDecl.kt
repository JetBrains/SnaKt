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
import viper.silver.plugin.standard.adt.AdtType

/**
 * Represents an ADT constructor on the Viper level.
 *
 * This declares a Viper constructor and has to be distinguished from a constructor application, for which
 * `AdtConstructorApp` should be used.
 */
data class AdtConstructorDecl(
    val name: SymbolicName,
    val adtName: SymbolicName,
    val formalArgs: List<Declaration.LocalVarDecl>,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.plugin.standard.adt.AdtConstructor> {
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.plugin.standard.adt.AdtConstructor = viper.silver.plugin.standard.adt.AdtConstructor(
            name.mangled,
            formalArgs.map { it.toSilver() }.toScalaSeq(),
            pos.toSilver(),
            info.toSilver(),
            AdtType(adtName.mangled, emptyScalaMap(), emptySeq()),
            adtName.mangled,
            trafos.toSilver(),
        )
}

data class AdtDecl(
    val name: SymbolicName,
    val constructors: List<AdtConstructorDecl>,
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