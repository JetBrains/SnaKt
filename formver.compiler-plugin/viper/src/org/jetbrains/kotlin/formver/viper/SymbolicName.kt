/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper


interface NamedEntity {
    context(nameResolver: NameResolver)
    fun fullName(): String?
    val candidates: List<CandidateName>
}

sealed interface NamePart {

    context(nameResolver: NameResolver)
    fun name(): String

    // Just simple string name
    class Basic(val name: String) : NamePart {
        context(nameResolver: NameResolver)
        override fun name(): String = name
    }

    // Depends on other Name
    class Dependent(val name: NamedEntity) : NamePart {
        context(nameResolver: NameResolver)
        override fun name(): String = nameResolver.resolve(name)
    }
}

class CandidateName(val parts: List<NamePart>) {

    context(nameResolver: NameResolver)
    fun name(): String {
        return parts.joinToString(SEPARATOR) { it.name() }
    }
}

class CandidatesBuilder {
    private val candidates = mutableListOf<CandidateName>()

    /**
     * Creates a new candidate using a nested DSL block.
     */
    fun candidate(init: CandidateNameBuilder.() -> Unit) {
        val builder = CandidateNameBuilder()
        builder.init()
        candidates.add(builder.build())
    }

    /**
     * Shorthand to add a single-part candidate directly.
     */
    fun candidate(name: String) {
        candidates.add(CandidateName(listOf(NamePart.Basic(name))))
    }

    fun build(): List<CandidateName> = candidates.toList()
}

/**
 * The top-level entry point for building a list of names.
 */
fun buildCandidates(init: CandidatesBuilder.() -> Unit): List<CandidateName> {
    return CandidatesBuilder().apply(init).build()
}

// Re-using the logic from the previous step for the individual parts
class CandidateNameBuilder {
    private val parts = mutableListOf<NamePart>()

    operator fun String.unaryPlus() {
        parts.add(NamePart.Basic(this))
    }

    operator fun NamedEntity.unaryPlus() {
        parts.add(NamePart.Dependent(this))
    }

    operator fun Iterable<NamedEntity>.unaryPlus() {
        parts.addAll(map { NamePart.Dependent(it) })
    }

    fun build(): CandidateName = CandidateName(parts.toList())
}



/**
 * Represents a Kotlin name with its Viper equivalent.
 *
 * We could directly convert names and pass them around as strings, but this
 * approach makes it easier to see where they came from during debugging.
 */
const val SEPARATOR = "$"

interface SymbolicName : NamedEntity {
    /**
     * Describes the type of the entity the names refer to.
     * This could be a property, a backing field, a getter, a setter, etc.
     */
    val nameType: NameType?
        get() = null
    context(nameResolver: NameResolver)
    val mangledScope: String?
        get() = null
    context(nameResolver: NameResolver)
    val mangledBaseName: String

    context(nameResolver: NameResolver)
    override fun fullName(): String? {
        return nameResolver.resolve(this)
    }
}

context(nameResolver: NameResolver)
val SymbolicName.mangled: String
    get() = nameResolver.resolve(this)

val SymbolicName.debugMangled: String
    get() {
        val debugResolver = DebugNameResolver()
        return debugResolver.resolve(this)
    }


/**
 * Collects all types of names we can have.
 */
sealed class NameType(val name: String) : NamedEntity {

    context(nameResolver: NameResolver)
    override fun fullName(): String? {
        return name
    }

    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +name
            }
        }
    object Property : NameType("p")
    object BackingField : NameType("bf")
    object Getter : NameType("g")
    object Setter : NameType("s")
    object ExtensionSetter : NameType("es")
    object ExtensionGetter : NameType("eg")
    object Type : NameType("t") {
        object Class : NameType("c")
    }
    object Constructor : NameType("con")
    object Function : NameType("f")
    object Predicate : NameType("p") // merge them with "generated"
    object Havoc : NameType("h")// merge them with "generated"
    sealed class Label(lblName: String) : NameType(lblName) {
        object Return : Label("ret")
        object Break : Label("break")
        object Continue : Label("cont")
        object Catch : Label("catch")
        object TryExit : Label("tryExit")
    }

    object Variables : NameType("v")
    object Domain : NameType("d")
    object DomainFunction : NameType("df")
    object Special : NameType("sp") // I think we should not have this. Like, what does special mean?
}