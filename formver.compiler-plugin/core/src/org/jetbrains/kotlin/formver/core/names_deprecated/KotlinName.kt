/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names_deprecated

import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.asTypeEmbedding
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SEPARATOR
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.mangled
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

    override fun dependsOn(): Set<SymbolicName> = emptySet()
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { name.asStringStripSpecialMarkers() }
    }
}

abstract class TypedKotlinName(override val mangledType: String, open val name: Name) : KotlinName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = name.asStringStripSpecialMarkers()

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { name.asStringStripSpecialMarkers() }
        yield { "${mangledType}_${name.asStringStripSpecialMarkers()}" }
    }

    override fun dependsOn(): Set<SymbolicName> = emptySet()
}

abstract class TypedKotlinNameWithType(override val mangledType: String, open val name: Name, val type: TypeEmbedding) :
    KotlinName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "${name.asStringStripSpecialMarkers()}$SEPARATOR${type.name.mangled}"
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { name.asStringStripSpecialMarkers() }
        yield { resolver -> "${name.asStringStripSpecialMarkers()}_${resolver.resolve(type.name)}" }
    }

    override fun dependsOn(): Set<SymbolicName> = setOf(type.name)
}

data class FunctionKotlinName(override val name: Name, val functionType: FunctionTypeEmbedding) :
    TypedKotlinNameWithType(
        "f", name,
        functionType.asTypeEmbedding()
    ) {

    override fun dependsOn(): Set<SymbolicName> = setOf(functionType.name)

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { name.asStringStripSpecialMarkers() }
        yield { resolver -> "${name.asStringStripSpecialMarkers()}_${resolver.resolve(type.name)}" }
    }
}

/**
 * This name will never occur in the viper output, but rather is used to lookup properties.
 */
data class PropertyKotlinName(override val name: Name) : TypedKotlinName("pp", name)
data class BackingFieldKotlinName(override val name: Name) : TypedKotlinName("bf", name) {

}

data class GetterKotlinName(override val name: Name) : TypedKotlinName("pg", name) {

}

data class SetterKotlinName(override val name: Name) : TypedKotlinName("ps", name) {

}
data class ExtensionSetterKotlinName(override val name: Name, val functionType: FunctionTypeEmbedding) :
    TypedKotlinNameWithType("es", name, functionType.asTypeEmbedding())
data class ExtensionGetterKotlinName(override val name: Name, val functionType: FunctionTypeEmbedding) :
    TypedKotlinNameWithType("eg", name, functionType.asTypeEmbedding()) {
    override fun dependsOn(): Set<SymbolicName> = emptySet()
}

data class ClassKotlinName(val name: FqName) : KotlinName {
    override val mangledType: String
        get() = "c"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = name.asViperString()

    constructor(classSegments: List<String>) : this(FqName.fromSegments(classSegments))

    override fun dependsOn(): Set<SymbolicName> = emptySet()
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { name.asViperString() }
        yield { "c_${name.asViperString()}" }
    }
}

data class ConstructorKotlinName(val type: FunctionTypeName) : KotlinName {
    override val mangledType: String
        get() = "con"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = type.mangledBaseName

    override fun dependsOn(): Set<SymbolicName> = setOf(type)
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { resolver -> "con${resolver.resolve(type.returnType)}" }
        yield { resolver -> "con${resolver.resolve(type)}" }
    }
}

// It's a bit of a hack to make this as KotlinName, it should really just be any old name, but right now our scoped
// names are KotlinNames and changing that could be messy.
data class PredicateKotlinName(val name: String) : KotlinName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = name
    override val mangledType: String
        get() = "p"

    override fun dependsOn(): Set<SymbolicName> = emptySet()

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { name }
        yield { "${mangledType}_${name}" }
    }
}

data class HavocKotlinName(val type: TypeEmbedding) : KotlinName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = type.name.mangled
    override val mangledType: String
        get() = "havoc"

    override fun dependsOn(): Set<SymbolicName> = emptySet()

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { "havoc" }
        yield { resolver -> "havoc_${resolver.resolve(type.name)}" }
    }
}


sealed interface TypeName : KotlinName


data class PretypeName(val name: String) : TypeName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = name

    override fun dependsOn(): Set<SymbolicName> = emptySet()
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { name }
    }
}

data class ListOfTypes(val names: List<TypeName>) : TypeName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = names.joinToString(SEPARATOR) { it.mangled }

    override fun dependsOn(): Set<SymbolicName> = emptySet()
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { resolver -> "(" + names.joinToString(", ") { resolver.resolve(it) } + ")" }
    }
}

data class SimpleTypeName(val pretype: PretypeEmbedding, val nullable: Boolean) : TypeName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = pretype.name.mangledBaseName
    override val mangledType: String
        get() = listOfNotNull(
            if (nullable) "N" else null,
            "T",
            if (pretype is FunctionTypeEmbedding) "F" else null
        ).joinToString("")

    override fun dependsOn(): Set<SymbolicName> = emptySet()
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { resolver -> "${resolver.resolve(pretype.name)}${if (nullable) "_N" else ""}" }
    }
}

data class FunctionTypeName(val args: ListOfTypes, val returnType: TypeName) : TypeName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = args.mangledBaseName + SEPARATOR + returnType.mangledBaseName

    override fun dependsOn(): Set<SymbolicName> = args.names.toSet() + returnType

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { resolver -> "${resolver.resolve(args)} -> ${resolver.resolve(returnType)}" }
    }
}