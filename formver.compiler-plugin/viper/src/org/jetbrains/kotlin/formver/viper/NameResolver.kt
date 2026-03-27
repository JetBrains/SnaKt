package org.jetbrains.kotlin.formver.viper

/**
 * Interface defining a strategy for converting a [ScopedKotlinName]
 * into an internal Viper identifier ([SymbolicName]).
 *
 * Multiple conversion strategies can be implemented and passed
 * to the `toViper(...)` function as needed.
 */

interface NameResolver {
    fun resolve(name: NamedEntity): String
    fun register(name: SymbolicName)
}

class DebugNameResolver : NameResolver {
    override fun resolve(name: NamedEntity): String = when (name) {
        is SymbolicName -> listOfNotNull(
            name.nameType?.fullName(),
            name.mangledScope,
            name.mangledBaseName
        ).joinToString(SEPARATOR)

        else -> name.toString()
    }

    override fun register(name: SymbolicName) {}
}