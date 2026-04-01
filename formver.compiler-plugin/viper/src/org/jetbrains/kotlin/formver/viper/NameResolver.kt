package org.jetbrains.kotlin.formver.viper

/**
 * Interface defining a strategy for converting a [ScopedKotlinName]
 * into an internal Viper identifier ([SymbolicName]).
 *
 * Multiple conversion strategies can be implemented and passed
 * to the `toViper(...)` function as needed.
 */

interface NameResolver {
    /**
     * Resolves the given [name] into a Viper identifier.
     */
    fun resolve(name: SymbolicName): String

    /**
     * Registers a new [name] for mangling.
     */
    fun register(name: SymbolicName)

    /**
     * Applies mangling to all registered names.
     */
    fun mangle() {}
}

class DebugNameResolver : NameResolver {
    override fun resolve(name: SymbolicName): String = listOfNotNull(
        name.nameType?.mangledName,
            name.mangledScope,
            name.mangledBaseName
        ).joinToString(SEPARATOR)

    override fun register(name: SymbolicName) {}
}