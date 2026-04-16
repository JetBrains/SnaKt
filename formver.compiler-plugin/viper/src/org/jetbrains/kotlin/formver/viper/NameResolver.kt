package org.jetbrains.kotlin.formver.viper

/**
 * Interface defining a strategy for converting a [SymbolicName]
 * into an internal Viper identifier (String).
 *
 * Multiple conversion strategies can be implemented and passed
 * to the `toViper(...)` function as needed.
 *
 * Workflow:
 *  1. Register all used names with [register]
 *  2. Call [resolve] to perform any necessary name resolution
 *  3. Use [lookup] to look up Viper identifiers
 */

interface NameResolver {
    /**
     * Given a [SymbolicName], returns the corresponding Viper identifier.
     * Must only be called after [resolve] was executed
     */
    fun lookup(name: SymbolicName): String

    /**
     * Performs any necessary resolution of names to make them collision free.
     */
    fun resolve()

    /**
     * Register a new name which later can be resolved by [lookup].
     */
    fun register(name: AnyName)
}
