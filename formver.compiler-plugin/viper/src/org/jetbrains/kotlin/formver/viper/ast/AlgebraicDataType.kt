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


data class AdtName(val className: SymbolicName) : SymbolicName {
    override val mangledType: String get() = "adt"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String get() = className.mangled
}

data class AdtConstructorName(val adtName: AdtName, val className: SymbolicName) : SymbolicName {
    override val mangledType: String get() = "adtc"

    context(nameResolver: NameResolver)
    override val mangledScope: String get() = adtName.mangledBaseName

    context(nameResolver: NameResolver)
    override val mangledBaseName: String get() = "constr_${className.mangled}"
}

/**
 * Represents any ADT constructor on the Viper level.
 *
 * It is used during the construction of the ADT type and also called during the construction of a new instance,
 * forming the link between the type itself and the usage in the program.
 */
data class AdtConstructor(
    val name: AdtConstructorName,
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
            AdtType(name.adtName.mangled, emptyScalaMap(), emptySeq()),
            name.adtName.mangled,
            trafos.toSilver(),
        )
}

/**
 * Abstract base for all ADT encodings.
 *
 * Each class type that can be declared as an ADT inherits this and defines the concrete ADT.
 */
abstract class AlgebraicDataType(
    val className: SymbolicName,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<Adt> {
    val name = AdtName(className)
    val includeInShortDump = true
    abstract val typeVars: List<Type.TypeVar>
    abstract val constructors: List<org.jetbrains.kotlin.formver.viper.ast.AdtConstructor>

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

    fun createConstructor(
        constructorClassName: SymbolicName,
        args: List<Declaration.LocalVarDecl> = emptyList(),
    ) = AdtConstructor(
        AdtConstructorName(name, constructorClassName),
        args,
    )
}

/** Represents an `object` with a default nullary constructor and no type parameters. */
class ConstantADT(
    className: SymbolicName,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) : AlgebraicDataType(className, pos, info, trafos) {
    override val typeVars: List<Type.TypeVar> = emptyList()
    override val constructors: List<org.jetbrains.kotlin.formver.viper.ast.AdtConstructor> = listOf(
        createConstructor(className),
    )
}