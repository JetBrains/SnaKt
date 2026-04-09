/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.asTypeEmbedding
import org.jetbrains.kotlin.formver.viper.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Name corresponding to an entity in the original Kotlin code.
 *
 * This is a little more general than the `Name` type in Kotlin: we also use this
 * to represent getters and setters, for example.
 */
sealed interface KotlinName : SymbolicName

data class SimpleKotlinName(val name: Name) : KotlinName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = name.asStringStripSpecialMarkers()
}

abstract class TypedKotlinName(override val mangledType: NameType, open val name: Name) : KotlinName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = name.asStringStripSpecialMarkers()
}

abstract class TypedKotlinNameWithType(
    override val mangledType: NameType,
    open val name: Name,
    val type: TypeEmbedding
) :
    KotlinName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "${name.asStringStripSpecialMarkers()}$SEPARATOR${type.name.mangled}"
}

data class FunctionKotlinName(override val name: Name, val functionType: FunctionTypeEmbedding) :
    TypedKotlinNameWithType(
        NameType.Function, name,
        functionType.asTypeEmbedding()
    )

/**
 * This name will never occur in the viper output, but rather is used to lookup properties.
 */
data class PropertyKotlinName(override val name: Name) : TypedKotlinName(NameType.Property, name)
data class BackingFieldKotlinName(override val name: Name) : TypedKotlinName(NameType.BackingField, name)
data class GetterKotlinName(override val name: Name) : TypedKotlinName(NameType.Getter, name)
data class SetterKotlinName(override val name: Name) : TypedKotlinName(NameType.Setter, name)
data class ExtensionSetterKotlinName(override val name: Name, val functionType: FunctionTypeEmbedding) :
    TypedKotlinNameWithType(NameType.ExtensionSetter, name, functionType.asTypeEmbedding())

data class ExtensionGetterKotlinName(override val name: Name, val functionType: FunctionTypeEmbedding) :
    TypedKotlinNameWithType(NameType.ExtensionGetter, name, functionType.asTypeEmbedding())

data class ClassKotlinName(val name: FqName) : KotlinName {
    override val mangledType: NameType
        get() = NameType.Type.Class

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = name.asViperString()

    constructor(classSegments: List<String>) : this(FqName.fromSegments(classSegments))
}

data class ConstructorKotlinName(val type: FunctionTypeEmbedding) : KotlinName {
    override val mangledType: NameType
        get() = NameType.Constructor

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = type.name.mangledBaseName
}

// It's a bit of a hack to make this as KotlinName, it should really just be any old name, but right now our scoped
// names are KotlinNames and changing that could be messy.
data class PredicateKotlinName(val name: String) : KotlinName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = name
    override val mangledType: NameType
        get() = NameType.Predicate
}

data class HavocKotlinName(val type: TypeEmbedding) : KotlinName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = type.name.mangled
    override val mangledType: NameType
        get() = NameType.Havoc
}

data class PretypeName(val name: String) : KotlinName {

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = name
}

data class SetOfNames(val names: List<SymbolicName>) : KotlinName {
    override val mangledType: NameType
        get() = NameType.Type

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = names.joinToString(SEPARATOR) { it.mangled }
}

data class TypeName(val pretype: PretypeEmbedding, val nullable: Boolean) : KotlinName {

    override val mangledType: NameType
        get() = NameType.Type

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = buildString {
            if (nullable) append("N")
            if (pretype is FunctionTypeEmbedding) append("F")
            if (isNotEmpty()) append(SEPARATOR)
            append(pretype.name.mangledBaseName)
        }

}