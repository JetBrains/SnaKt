package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import kotlin.reflect.KClass

/**
 * Attribute to annotate the locality of a type.
 * The presence of this attribute signals that the reference is `Local`, while the absence signals that it is `Global`.
 */
object ConeLocalAttribute : ConeAttribute<ConeLocalAttribute>() {
    override val key: KClass<ConeLocalAttribute>
        get() = ConeLocalAttribute::class

    override val keepInInferredDeclarationType: Boolean
        get() = true

    override fun union(other: ConeLocalAttribute?): ConeLocalAttribute {
        return other ?: this
    }

    override fun intersect(other: ConeLocalAttribute?): ConeLocalAttribute? {
        return other
    }

    override fun add(other: ConeLocalAttribute?): ConeLocalAttribute? {
        return other
    }

    override fun isSubtypeOf(other: ConeLocalAttribute?): Boolean {
        return false
    }

    override fun toString(): String {
        return "@Local"
    }
}

fun ConeLocalAttribute?.union(other: ConeLocalAttribute?): ConeLocalAttribute? =
    this?.union(other) ?: other

fun ConeLocalAttribute?.intersect(other: ConeLocalAttribute?): ConeLocalAttribute? =
    this?.intersect(other)

val ConeKotlinType.localAttribute: ConeLocalAttribute?
    get() = attributes[ConeLocalAttribute::class]

fun ConeLocalAttribute?.accepts(other: ConeLocalAttribute?): Boolean =
    when (this) {
        null -> other == null
        else -> true
    }

fun ConeLocalAttribute?.render(): String =
    if (this == null)
        "global"
    else
        "local"
