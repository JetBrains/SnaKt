package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import kotlin.reflect.KClass

/**
 * Attribute to annotate the locality of a type.
 * The presence of this attribute signals that the reference is `Local`, while the absence signals that it is `Global`.
 */
data class ConeLocalityAttribute(
    val owner: FirDeclaration? = null
) : ConeAttribute<ConeLocalityAttribute>() {
    override val key: KClass<ConeLocalityAttribute>
        get() = ConeLocalityAttribute::class

    override val keepInInferredDeclarationType: Boolean
        get() = true

    override fun union(other: ConeLocalityAttribute?): ConeLocalityAttribute {
        return other ?: this
    }

    override fun intersect(other: ConeLocalityAttribute?): ConeLocalityAttribute? {
        return other
    }

    override fun add(other: ConeLocalityAttribute?): ConeLocalityAttribute? {
        return other
    }

    override fun isSubtypeOf(other: ConeLocalityAttribute?): Boolean {
        return false
    }

    override fun toString(): String {
        return "@local(${owner?.symbol ?: "unknown"})"
    }
}

fun ConeLocalityAttribute?.union(other: ConeLocalityAttribute?): ConeLocalityAttribute? =
    this?.union(other) ?: other

val ConeKotlinType.localAttribute: ConeLocalityAttribute?
    get() = attributes[ConeLocalityAttribute::class]

fun ConeLocalityAttribute?.accepts(other: ConeLocalityAttribute?): Boolean =
    when (this) {
        null -> other == null
        else -> when (other) {
            null -> true
            else -> this.owner == other.owner
        }
    }

fun ConeLocalityAttribute?.render(): String =
    if (this == null)
        "global"
    else
        "local(${(owner?.symbol as? FirCallableSymbol<*>)?.name ?: "unknown"})"
