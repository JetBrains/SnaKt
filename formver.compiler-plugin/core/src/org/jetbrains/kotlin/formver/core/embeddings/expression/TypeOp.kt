/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.types.*
import org.jetbrains.kotlin.utils.addIfNotNull

data class Is(
    val inner: ExpEmbedding, val comparisonType: RuntimeTypeHolder,
    override val sourceRole: SourceRole? = null,
) :
    ExpEmbedding {
    override val type = buildType { boolean() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIs(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(inner)
}


/**
 * ExpEmbedding to change the TypeEmbedding of an inner ExpEmbedding.
 * This is needed since most of our invariants require type and hence can be made more precise via Cast.
 */
data class Cast(val inner: ExpEmbedding, override val type: TypeEmbedding) : ExpEmbedding {
    override fun ignoringCasts(): ExpEmbedding = inner.ignoringCasts()
    override fun ignoringCastsAndMetaNodes(): ExpEmbedding = inner.ignoringCastsAndMetaNodes()

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitCast(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(inner)
}

fun ExpEmbedding.withType(newType: TypeEmbedding): ExpEmbedding = if (type == newType) this else Cast(this, newType)

fun ExpEmbedding.withType(init: TypeBuilder.() -> PretypeBuilder): ExpEmbedding = withType(buildType(init))


/**
 * Implementation of "safe as".
 *
 * We do some special-purpose logic here depending on whether the receiver is nullable or not, hence we cannot use `InhaleProven` directly.
 * This is also why we insist the result is stored; this is a little stronger than necessary, but that does not harm correctness.
 */
data class SafeCast(val exp: ExpEmbedding, val targetType: TypeEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding
        get() = targetType.getNullable()

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(exp)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitSafeCast(this)
}

interface InhaleInvariants : ExpEmbedding {
    val invariants: List<TypeInvariantEmbedding>
    val exp: ExpEmbedding

    override val type: TypeEmbedding
        get() = exp.type

    val simplified: ExpEmbedding
        get() = if (invariants.isEmpty()) exp
        else this

    // TODO: add children() override (sequenceOf(exp)) once we fix corresponding tests
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitInhaleInvariants(this)
}

/**
 * Augment this expression with all invariants of a certain kind that we know about the type.
 *
 * This may require storing the result in a variable, if it is not already a variable. The `simplified` property allows
 * unwrapping this type when this can be avoided.
 */
private data class InhaleInvariantsForExp(
    override val exp: ExpEmbedding,
    override val invariants: List<TypeInvariantEmbedding>
) :
    InhaleInvariants

private data class InhaleInvariantsForVariable(
    override val exp: ExpEmbedding,
    override val invariants: List<TypeInvariantEmbedding>,
) :
    InhaleInvariants

class InhaleInvariantsBuilder(val exp: ExpEmbedding) {
    val invariants = mutableListOf<TypeInvariantEmbedding>()

    fun complete(ctx: TypeResolver): ExpEmbedding {
        if (proven) exp.type.subTypeInvariant().let { invariants.add(it) }
        if (access) {
            invariants.addAll(exp.type.accessInvariants(ctx))
            invariants.addIfNotNull(exp.type.sharedPredicateAccessInvariant(ctx))
        }
        return when (exp.underlyingVariable) {
            null -> InhaleInvariantsForExp(exp, invariants)
            else -> InhaleInvariantsForVariable(exp, invariants)
        }.simplified
    }

    var proven: Boolean = false
    var access: Boolean = false
}

inline fun ExpEmbedding.withInvariants(ctx: TypeResolver, block: InhaleInvariantsBuilder.() -> Unit): ExpEmbedding {
    val builder = InhaleInvariantsBuilder(this)
    builder.block()
    return builder.complete(ctx)
}

fun ExpEmbedding.withIsUnitInvariantIfUnit(ctx: TypeResolver) = withInvariants(ctx) {
    proven = type.equalToType { unit() }
}

inline fun ExpEmbedding.withNewTypeInvariants(
    newType: TypeEmbedding,
    ctx: TypeResolver,
    block: InhaleInvariantsBuilder.() -> Unit
) =
    if (this.type == newType) this else withType(newType).withInvariants(ctx, block)
