/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoSilver
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.formver.viper.toScalaMap
import org.jetbrains.kotlin.formver.viper.toScalaSeq
import viper.silver.ast.*

/**
 * Sealed hierarchy of Viper types.
 *
 * Each entry corresponds to a Silver type and implements [IntoSilver] to produce the
 * matching Scala type object required by the Silicon API.
 *
 * All types support [substitute], which replaces any [TypeVar] instances according to the
 * supplied mapping; concrete types (e.g. [Int], [Bool]) return themselves unchanged.
 */
sealed interface Type : IntoSilver<viper.silver.ast.Type> {

    /**
     * Returns a copy of this type with each [TypeVar] occurrence replaced by its entry in
     * [typeVarMap].  [TypeVar]s not present in the map are left unchanged.
     */
    fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Type

    /** Viper's built-in mathematical integer type. */
    data object Int : Type {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Type = `Int$`.`MODULE$`
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Int = Int
    }

    /** Viper's built-in Boolean type. */
    data object Bool : Type {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Type = `Bool$`.`MODULE$`
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Bool = Bool
    }

    /** Viper's built-in permission-amount type (rational values in [0, 1]). */
    data object Perm : Type {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Type = `Perm$`.`MODULE$`
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Perm = Perm
    }

    /**
     * Viper's heap reference type.
     *
     * All object-typed Kotlin values are represented as [Ref] in the Viper encoding.
     */
    data object Ref : Type {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Type = `Ref$`.`MODULE$`
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Ref = Ref
    }

    /** Viper's magic-wand type, used for advanced separation-logic proofs. */
    data object Wand : Type {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Type = `Wand$`.`MODULE$`
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Wand = Wand
    }

    /** Viper's built-in immutable sequence type `Seq[elemType]`. */
    data class Seq(val elemType: Type) : Type {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Type = SeqType.apply(elemType.toSilver())
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Seq =
            Seq(elemType.substitute(typeVarMap))
    }

    /** Viper's built-in immutable set type `Set[elemType]`. */
    data class Set(val elemType: Type) : Type {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Type = SetType.apply(elemType.toSilver())
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Set =
            Set(elemType.substitute(typeVarMap))
    }

    /** Viper's built-in multiset type `Multiset[elemType]`. */
    data class Multiset(val elemType: Type) : Type {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Type = MultisetType.apply(elemType.toSilver())
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Multiset =
            Multiset(elemType.substitute(typeVarMap))
    }

    /** Viper's built-in immutable map type `Map[keyType, valueType]`. */
    data class Map(val keyType: Type, val valueType: Type) : Type {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Type = MapType.apply(keyType.toSilver(), valueType.toSilver())
        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Map =
            Map(keyType.substitute(typeVarMap), valueType.substitute(typeVarMap))

    }

    /**
     * A Viper type variable (generic type parameter) identified by [name].
     *
     * Type variables appear in polymorphic domain declarations and are eliminated by
     * [substitute] when the domain is instantiated with concrete types.
     */
    data class TypeVar(val name: String) : Type {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.TypeVar =
            viper.silver.ast.TypeVar(name)

        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>) = typeVarMap.getOrDefault(this, this)
    }

    /**
     * A use of a declared Viper [Domain] as a type.
     *
     * @property domainName       The name of the domain being used as a type.
     * @property typeParams       The generic type parameters declared by the domain.
     * @property typeSubstitutions The concrete type assigned to each [typeParams] entry at this
     *                             use site; defaults to an identity map (each variable maps to itself).
     */
    data class Domain(
        val domainName: DomainName,
        val typeParams: List<TypeVar> = emptyList(),
        val typeSubstitutions: kotlin.collections.Map<TypeVar, Type> = emptyMap(),
    ) : Type {
        context(nameResolver: NameResolver)
        override fun toSilver(): DomainType =
            DomainType.apply(
                domainName.mangled,
                typeSubstitutions.mapKeys { it.key.toSilver() }.mapValues { it.value.toSilver() }.toScalaMap(),
                typeParams.map { it.toSilver() }.toScalaSeq()
            )

        override fun substitute(typeVarMap: kotlin.collections.Map<TypeVar, Type>): Domain =
            Domain(
                domainName,
                typeParams,
                typeParams.associateWith { typeSubstitutions.getOrDefault(it, it).substitute(typeVarMap) })

    }

}
