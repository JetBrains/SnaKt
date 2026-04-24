/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.names.NameMatcher
import org.jetbrains.kotlin.formver.core.names.ScopedName
import org.jetbrains.kotlin.formver.names.debugMangled
import org.jetbrains.kotlin.formver.viper.ast.DomainFunc
import org.jetbrains.kotlin.formver.viper.ast.Exp

// TODO: incorporate generic parameters.
data class ClassTypeEmbedding(override val name: ScopedName) : PretypeEmbedding {

    override val runtimeType: Exp = this.embedClassTypeFunc()()

    override fun accessInvariants(ctx: TypeResolver): List<TypeInvariantEmbedding> =
        ctx.details(name).accessInvariants(ctx)

    override fun sharedPredicateAccessInvariant(ctx: TypeResolver) =
        ctx.details(name).sharedPredicateAccessInvariant(ctx)

    override fun uniquePredicateAccessInvariant(ctx: TypeResolver) =
        ctx.details(name).uniquePredicateAccessInvariant(ctx)
}

fun PretypeEmbedding.isInheritorOfCollectionTypeNamed(name: String, ctx: TypeResolver): Boolean {
    val classEmbedding = this as? ClassTypeEmbedding ?: return false
    return isCollectionTypeNamed("Collection") || ctx.lookupSuperTypes(this.name).any {
        it.isInheritorOfCollectionTypeNamed(name, ctx)
    }
}

fun PretypeEmbedding.isCollectionInheritor(ctx: TypeResolver) = isInheritorOfCollectionTypeNamed("Collection", ctx)

fun PretypeEmbedding.isCollectionTypeNamed(name: String): Boolean {
    val classEmbedding = this as? ClassTypeEmbedding ?: return false
    NameMatcher.Companion.matchGlobalScope(classEmbedding.name) {
        ifInCollectionsPkg {
            ifClassName(name) {
                return true
            }
        }
        return false
    }
}

fun ClassTypeEmbedding.embedClassTypeFunc(): DomainFunc = RuntimeTypeDomain.classTypeFunc(name)

fun ClassTypeEmbedding.predicateAccess(
    receiver: ExpEmbedding,
    ctx: TypeResolver,
    source: KtSourceElement?
): Exp.PredicateAccess {
    val access = (sharedPredicateAccessInvariant(ctx)?.fillHole(receiver)
        ?.pureToViper(toBuiltin = true, ctx, source) as? Exp.PredicateAccess
        ?: error("Translating shared predicate of ${name.debugMangled} yielded no predicate access."))
    return access
}