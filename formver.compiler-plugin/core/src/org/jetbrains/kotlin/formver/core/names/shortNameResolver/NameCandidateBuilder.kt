package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.viper.AnyName

/**
 * Represents a part of a candidate name.
 */
sealed interface NamePart {

    /**
     * A simple string name part.
     */
    data class Basic(val name: String) : NamePart

    /**
     * A name part that depends on another [AnyName].
     */
    data class Dependent(val name: AnyName) : NamePart

    /**
     * A separator between name parts.
     */
    object Separator : NamePart
}

/**
 * A candidate name composed of multiple [NamePart]s.
 * There are no public available classes. A candidate name must always be created using [CandidateNameBuilder].
 */
interface CandidateName {
    fun moveableParts(): List<NamePart.Dependent> = parts.filterIsInstance<NamePart.Dependent>()
    val parts: List<NamePart>
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
        candidates.add(CandidateNameBuilder().apply { +name }.build())
    }

    /**
     * Adds multiple candidates from a [List] of [CandidateName]s.
     */
    operator fun List<CandidateName>.unaryPlus() {
        candidates.addAll(this)
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
 *
 * Separators are automatically added between parts, except when explicitly skipped (use +noSeparator).
 */
class CandidateNameBuilder {
    private val parts = mutableListOf<NamePart>()

    private object SkipSeparator : NamePart

    /**
     * Prevents the addition of a separator before the next part.
     */
    val noSeparator: Unit
        get() {
            parts.add(SkipSeparator)
        }

    /**
     * Adds a string part to the name.
     */
    operator fun String.unaryPlus() {
        parts.add(NamePart.Basic(this))
    }

    /**
     * Adds a named entity part to the name.
     */
    operator fun AnyName.unaryPlus() {
        parts.add(NamePart.Dependent(this))
    }

    /**
     * Adds all parts from an iterable to the name.
     */
    operator fun Iterable<*>.unaryPlus() {
        this.forEach {
            when (it) {
                is String -> +it
                is AnyName -> +it
                is Iterable<*> -> +it
                null -> {}
                else -> throw SnaktInternalException(null, "Unsupported type: ${it::class.simpleName}")
            }
        }
    }

    /**
     * Builds the [CandidateName], automatically inserting [NamePart.Separator]s
     * between parts unless [noSeparator] was used.
     */
    fun build(): CandidateName {
        val separatorParts = parts.foldRight(Pair(mutableListOf<NamePart>(), false)) { part, (result, addSeparator) ->
            if (part is SkipSeparator) {
                return@foldRight Pair(result, false)
            }
            if (addSeparator) {
                result.add(NamePart.Separator)
            }
            result.add(part)
            Pair(result, true)
        }

        return object : CandidateName {
            override val parts: List<NamePart> = separatorParts.first.reversed()
        }
    }
}
