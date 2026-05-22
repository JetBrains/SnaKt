/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.kotlinClassId
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.ast.DomainFunc
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Predicate


interface ClassTypeEmbedding : PretypeEmbedding {
    override val name: ScopedName

    context(ctx: TypeResolver)
    fun uniquePredicate(): Predicate
    val uniquePredicateName
        get() = ScopedName(name.asScope(), PredicateName("unique"))
}

// TODO: incorporate generic parameters.
data class ClassTypeEmbeddingImpl(override val name: ScopedName) : ClassTypeEmbedding {

    override val runtimeType: Exp = this.embedClassTypeFunc()()



    context(ctx: TypeResolver)
    override fun uniquePredicate(): Predicate = ClassPredicateBuilder.build(name, uniquePredicateName) {
        includeSubTypeInvariants()
        forEachPropertyField {
            forBackingField {
                if (!isAlwaysWriteable) {
                    addAccessPermissions(PermExp.FullPerm())

                    forType {
                        includeSubTypeInvariants()
                    }
                }
            }
            forType {
                if (isUnique) {
                    addAccessToUniquePredicate()
                }
                includeSubTypeInvariants()
            }
        }
        forEachSuperType {
            addAccessToUniquePredicate()
        }
    }

    override fun accessInvariants(ctx: TypeResolver): List<TypeInvariantEmbedding> =
        ctx.flatMapUniqueFields(name) { field ->
            field.accessInvariantsForParameter()
        }

    override fun uniquePredicateAccessInvariant(ctx: TypeResolver) =
        PredicateAccessTypeInvariantEmbedding(uniquePredicateName, PermExp.FullPerm())

}


fun ClassTypeEmbedding.embedClassTypeFunc(): DomainFunc = RuntimeTypeDomain.classTypeFunc(name)

fun ClassTypeEmbedding.predicateAccess(
    receiver: ExpEmbedding, ctx: TypeResolver, source: KtSourceElement?
): Exp.PredicateAccess {
    val access = (uniquePredicateAccessInvariant(ctx)?.fillHole(receiver)
        ?.pureToViper(toBuiltin = true, ctx, source) as? Exp.PredicateAccess
        ?: error("Translating shared predicate of ${name.debugMangled} yielded no predicate access."))
    return access
}


data object IntArrayTypeEmbedding : ClassTypeEmbedding {
    override val name = kotlinClassId("IntArray").embedName()
    override val runtimeType = RuntimeTypeDomain.intArrayType()

    context(ctx: TypeResolver)
    override fun uniquePredicate(): Predicate = ClassPredicateBuilder.build(name, uniquePredicateName) {}
}
