package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.viper.NamedEntity

/**
 * Represents a part of a candidate name.
 */
sealed interface NamePart {

    /**
     * A simple string name part.
     */
    class Basic(val name: String) : NamePart

    /**
     * A name part that depends on another [NamedEntity].
     */
    class Dependent(val name: NamedEntity) : NamePart

    /**
     * A separator between name parts.
     */
    object Separator : NamePart

    /**
     * Indicates that no separator should be added before this part.
     */
    object NoSeparator : NamePart
}

/**
 * A candidate name composed of multiple [NamePart]s.
 */
class CandidateName(val parts: List<NamePart>) {

    /**
     * Returns the list of [NamePart.Dependent] parts in this candidate name.
     */
    fun moveableParts(): List<NamePart.Dependent> = parts.filterIsInstance<NamePart.Dependent>()
}

/**
 * A builder for a list of [CandidateName]s.
 */
class CandidatesBuilder {
    private val candidates = mutableListOf<CandidateName>()

    /**
     * Creates a new candidate using a nested DSL block.
     *
     * @param init The initialization block for the [CandidateNameBuilder].
     */
    fun candidate(init: CandidateNameBuilder.() -> Unit) {
        val builder = CandidateNameBuilder()
        builder.init()
        candidates.add(builder.build())
    }

    /**
     * Shorthand to add a single-part candidate directly.
     *
     * @param name The string name to add as a basic part.
     */
    fun candidate(name: String) {
        candidates.add(CandidateName(listOf(NamePart.Basic(name))))
    }

    /**
     * Builds the list of [CandidateName]s.
     */
    fun build(): List<CandidateName> = candidates.toList()
}

/**
 * The top-level entry point for building a list of candidate names.
 *
 * @param init The initialization block for the [CandidatesBuilder].
 * @return A list of [CandidateName]s.
 */
fun buildCandidates(init: CandidatesBuilder.() -> Unit): List<CandidateName> {
    return CandidatesBuilder().apply(init).build()
}

/**
 * A builder for a single [CandidateName], providing a DSL for adding parts.
 */
class CandidateNameBuilder {
    private val parts = mutableListOf<NamePart>()

    /**
     * Prevents the addition of a separator before the next part.
     */
    val noSeparator: Unit
        get() {
            parts.add(NamePart.NoSeparator)
        }

    /**
     * Adds a [NamePart.Basic] part to the name.
     */
    operator fun String.unaryPlus() {
        parts.add(NamePart.Basic(this))
    }

    /**
     * Adds a [NamePart.Dependent] part to the name.
     */
    operator fun NamedEntity.unaryPlus() {
        parts.add(NamePart.Dependent(this))
    }

    /**
     * Adds multiple parts from an [Iterable].
     *
     * Supported types are [String] and [NamedEntity].
     *
     * @throws SnaktInternalException if an unsupported type is encountered.
     */
    operator fun Iterable<*>.unaryPlus() {
        parts.addAll(mapNotNull {
            when (it) {
                is String -> NamePart.Basic(it)
                is NamedEntity -> NamePart.Dependent(it)
                null -> null
                else -> throw SnaktInternalException(null, "Unsupported type: ${it::class.simpleName}")
            }
        })
    }

    /**
     * Builds the [CandidateName], automatically inserting [NamePart.Separator]s
     * between parts unless [noSeparator] was used.
     */
    fun build(): CandidateName {
        val separatorParts = parts.foldRight(Pair(mutableListOf<NamePart>(), false)) { part, (result, addSeparator) ->
            if (part is NamePart.NoSeparator) {
                return@foldRight Pair(result, false)
            }
            if (addSeparator) {
                result.add(NamePart.Separator)
            }
            result.add(part)
            Pair(result, true)
        }

        return CandidateName(separatorParts.first.reversed())
    }
}
