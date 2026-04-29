package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.CandidateName
import org.jetbrains.kotlin.formver.viper.NamePart


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
        val builder = CandidateNameBuilder(true)
        builder.init()
        candidates.add(builder.build())
    }

    /**
     * Creates a new candidate using a nested DSL block.
     * No separator is added between parts.
     *
     * @param init The initialization block for the [CandidateNameBuilder].
     */
    fun candidateNoSeparator(init: CandidateNameBuilder.() -> Unit) {
        val builder = CandidateNameBuilder(false)
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
 */
class CandidateNameBuilder(val separator: Boolean = true) {
    private val parts = mutableListOf<NamePart>()


    /**
     * Adds a string part to the name.
     */
    operator fun String.unaryPlus() {
        if (separator && parts.isNotEmpty()) parts.add(NamePart.Separator)
        parts.add(NamePart.Basic(this))
    }

    /**
     * Adds a named entity part to the name.
     */
    operator fun AnyName.unaryPlus() {
        if (separator && parts.isNotEmpty()) parts.add(NamePart.Separator)
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
     * Builds the [CandidateName].
     */
    fun build(): CandidateName {
        return CandidateName(parts)
    }
}
