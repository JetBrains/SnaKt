package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName

/**
 * Resolver that gives every name its fully-disambiguated form, without
 * any collision tracking.
 *
 * Used as a fallback / debug resolver. See
 * [org.jetbrains.kotlin.formver.core.names.ShortNameResolver] for the
 * collision-aware version.
 */
class SimpleNameResolver : NameResolver {

    val cache = mutableMapOf<AnyName, String>()

    override fun lookup(name: SymbolicName): String = cache.getOrPut(name) { name.longName() }

    override fun resolve() {}

    override fun register(name: AnyName) {}
}

val SymbolicName.debugMangled: String
    get() = longName()
