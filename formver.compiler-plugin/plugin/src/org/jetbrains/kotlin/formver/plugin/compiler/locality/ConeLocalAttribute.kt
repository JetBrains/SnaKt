package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.declaration
import kotlin.reflect.KClass

/**
 * Attribute to annotate the locality of a type.
 * The presence of this attribute signals that the reference is `Local`, while the absence signals that it is `Global`.
 */
data class ConeLocalAttribute(
    val declaration: FirDeclaration? = null
) : ConeAttribute<ConeLocalAttribute>() {
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
        return "@local(${declaration?.symbol ?: "unknown"})"
    }
}

fun ConeLocalAttribute?.union(other: ConeLocalAttribute?): ConeLocalAttribute? =
    this?.union(other) ?: other

val ConeKotlinType.localAttribute: ConeLocalAttribute?
    get() = attributes[ConeLocalAttribute::class]

fun ConeLocalAttribute?.accepts(other: ConeLocalAttribute?): Boolean =
    when (this) {
        null -> other == null
        else -> when (other) {
            null -> true
            else -> this.declaration == other.declaration
        }
    }

fun ConeLocalAttribute?.render(): String =
    if (this == null)
        "global"
    else
        "local(${(declaration?.symbol as? FirCallableSymbol<*>)?.name ?: "unknown"})"

context(context: CheckerContext)
val FirVariable.resolvedLocalAttribute: ConeLocalAttribute?
    get() = returnTypeRef.coneType.localAttribute?.copy(declaration = declaration)
