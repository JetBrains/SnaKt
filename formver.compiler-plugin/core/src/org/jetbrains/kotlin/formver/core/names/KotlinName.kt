/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.asTypeEmbedding
import org.jetbrains.kotlin.formver.core.names.shortNameResolver.buildCandidates
import org.jetbrains.kotlin.formver.core.names.shortNameResolver.nameOnlyCandidates
import org.jetbrains.kotlin.formver.core.names.shortNameResolver.nameWithPrefixCandidates
import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.CandidateName
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

    override val candidates: List<CandidateName> = nameOnlyCandidates(name.asStringStripSpecialMarkers())

    override val children: List<AnyName> = emptyList()
}

abstract class TypedKotlinName(override val nameType: NameType, open val name: Name) : KotlinName {
    override val candidates: List<CandidateName>
        get() = nameWithPrefixCandidates(name.asStringStripSpecialMarkers(), nameType)

    override val children: List<AnyName> = listOf(nameType)
}

abstract class TypedKotlinNameWithType(
    override val nameType: NameType, open val name: Name, val type: TypeEmbedding
) : KotlinName {
    override val candidates: List<CandidateName>
        get() = buildCandidates {
            +nameWithPrefixCandidates(name.asStringStripSpecialMarkers(), nameType)
            candidate {
                +nameType
                +name.asStringStripSpecialMarkers()
                +type.name
            }
        }

    override val children: List<AnyName> = listOf(nameType, type.name)
}

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

    override val candidates: List<CandidateName> = buildCandidates {
        val className = name.asString()
        candidate {
            +className
        }
        candidate {
            +nameType
            +className
        }
    }

    override val children: List<AnyName> = listOf(nameType)
}

data class ConstructorKotlinName(val type: FunctionTypeEmbedding) : KotlinName {
    override val inViper: Boolean = false
    override val nameType: NameType = NameType.Base.Constructor

    override val candidates: List<CandidateName> = buildCandidates {
        candidate {
            +nameType
        }
        candidate {
            +nameType
            +type.returnType.name
        }
        candidate {
            +nameType
            +type.returnType.name
            +"args"
            +type.paramTypes.map { it.name }
        }
    }

    override val children: List<AnyName> = listOf(nameType)
}

data class PretypeName(val name: String) : SymbolicName {
    override val inViper: Boolean = false

    override val candidates: List<CandidateName> = nameOnlyCandidates(name)

    override val children: List<AnyName> = emptyList()
}

data class ListOfNames<T : SymbolicName>(val names: List<T>) : SymbolicName {
    override val nameType: NameType = NameType.TypeCategory.GeneralType
    override val inViper: Boolean = false

    override val candidates: List<CandidateName> = buildCandidates {
        candidate {
            +names
        }
        candidate {
            +nameType
            +names
        }
    }

    override val children: List<AnyName> = names + listOf(nameType)
}

data class FunctionTypeName(val args: ListOfNames<TypeName>, val returns: TypeName) : SymbolicName {
    override val inViper: Boolean = false

    override val candidates: List<CandidateName> = buildCandidates {
        candidate {
            +returns
        }
        candidate {
            +"args"
            +args
            +"ret"
            +returns
        }
    }

    override val children: List<AnyName> = listOf(args, returns)
}

data class TypeName(val pretype: PretypeEmbedding, val nullable: Boolean) : SymbolicName {
    override val nameType: NameType = NameType.TypeCategory.GeneralType
    override val inViper: Boolean = false

    override val candidates: List<CandidateName> = buildCandidates {
        candidate {
            +pretype.name
        }
        candidateNoSeparator {
            if (nullable) +"N"
            +pretype.name
        }
    }

    override val children: List<AnyName> = listOf(nameType)
}
