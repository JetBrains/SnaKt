package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.types.ConeAttribute
import kotlin.reflect.KClass

class UniquenessConeAttribute(val type : UniquenessType) : ConeAttribute<UniquenessConeAttribute>() {
    override val key: KClass<UniquenessConeAttribute>
        get() = UniquenessConeAttribute::class

    override val keepInInferredDeclarationType: Boolean
        get() = true

    override val implementsEquality: Boolean
        get() = true

    override fun union(other: UniquenessConeAttribute?): UniquenessConeAttribute? {
        return UniquenessConeAttribute(type.join(other?.type ?: return null))
    }

    override fun intersect(other: UniquenessConeAttribute?): UniquenessConeAttribute? {
        return UniquenessConeAttribute(type.join(other?.type ?: return null))
    }

    override fun add(other: UniquenessConeAttribute?): UniquenessConeAttribute? {
        return UniquenessConeAttribute(type.join(other?.type ?: return null))
    }

    override fun isSubtypeOf(other: UniquenessConeAttribute?): Boolean {
        return true
    }

    override fun toString(): String {
        return "@(${type})"
    }
}
