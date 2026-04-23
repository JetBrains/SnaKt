/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.asTypeEmbedding
import org.jetbrains.kotlin.formver.viper.SymbolicName
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
    override val inViper: Boolean = false
}

abstract class TypedKotlinName(override val nameType: NameType, open val name: Name) : KotlinName

abstract class TypedKotlinNameWithType(
    override val nameType: NameType, open val name: Name, val type: TypeEmbedding
) : KotlinName

data class FunctionKotlinName(override val name: Name, val functionType: FunctionTypeEmbedding) :
    TypedKotlinNameWithType(
        NameType.Base.Function, name, functionType.asTypeEmbedding()
    ) {
    override val inViper: Boolean = false
}

/**
 * This name will never occur in the viper output, but rather is used to lookup properties.
 */
data class PropertyKotlinName(override val name: Name) : TypedKotlinName(NameType.Member.Property, name) {
    override val inViper: Boolean = false
}

data class BackingFieldKotlinName(override val name: Name) : TypedKotlinName(NameType.Member.BackingField, name) {
    override val inViper: Boolean = false
}

data class GetterKotlinName(override val name: Name) : TypedKotlinName(NameType.Member.Getter, name) {
    override val inViper: Boolean = false
}

data class SetterKotlinName(override val name: Name) : TypedKotlinName(NameType.Member.Setter, name) {
    override val inViper: Boolean = false
}

data class ExtensionSetterKotlinName(override val name: Name, val functionType: FunctionTypeEmbedding) :
    TypedKotlinNameWithType(NameType.Member.ExtensionSetter, name, functionType.asTypeEmbedding()) {
    override val inViper: Boolean = false
}


data class ExtensionGetterKotlinName(override val name: Name, val functionType: FunctionTypeEmbedding) :
    TypedKotlinNameWithType(NameType.Member.ExtensionGetter, name, functionType.asTypeEmbedding()) {
    override val inViper: Boolean = false
}


data class ClassKotlinName(val name: FqName) : KotlinName {
    override val inViper: Boolean = false
    override val nameType: NameType = NameType.TypeCategory.Class

    constructor(classSegments: List<String>) : this(FqName.fromSegments(classSegments))
}

data class ConstructorKotlinName(val type: FunctionTypeEmbedding) : KotlinName {
    override val inViper: Boolean = false
    override val nameType: NameType = NameType.Base.Constructor
}

data class PretypeName(val name: String) : SymbolicName {
    override val inViper: Boolean = false
}

data class ListOfNames<T : SymbolicName>(val names: List<T>) : SymbolicName {
    override val nameType: NameType = NameType.TypeCategory.GeneralType
    override val inViper: Boolean = false
}

data class FunctionTypeName(val args: ListOfNames<TypeName>, val returns: TypeName) : SymbolicName {
    override val inViper: Boolean = false
}

data class TypeName(val pretype: PretypeEmbedding, val nullable: Boolean) : SymbolicName {
    override val nameType: NameType = NameType.TypeCategory.GeneralType
    override val inViper: Boolean = false
}
