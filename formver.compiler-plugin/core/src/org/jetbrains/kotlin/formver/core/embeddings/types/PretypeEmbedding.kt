/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.conversion.ArrayCellDataFieldEmbedding
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.domains.domainVar
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.name.FqName

/**
 * A representation of a Kotlin type without nullability and uniqueness information.
 *
 * We explicitly choose not to make this a subtype of `TypeEmbedding`, even though there is a simple way of treating
 * every `PretypeEmbedding` as a `TypeEmbedding`: the goal of the separation into types and pretypes is to avoid
 * one showing up where the other is expected.  For example, the naming systems are different, and the equality
 * comparisons would not work.
 *
 * All pretype embeddings must be `data` classes or objects!
 */
interface PretypeEmbedding : RuntimeTypeHolder, TypeInvariantHolder {
    val name: SymbolicName

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = PlaintextLeaf(name.mangled)

    override fun subTypeInvariant(): TypeInvariantEmbedding = SubTypeInvariantEmbedding(this)
}

data object UnitTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.unitType()
    override val name = PretypeName("Unit")
}

data object NothingTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.nothingType()
    override val name = PretypeName("Nothing")

    override fun pureInvariants(): List<TypeInvariantEmbedding> = listOf(FalseTypeInvariant)
}

data object AnyTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.anyType()
    override val name = PretypeName("Any")
}

data object IntTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.intType()
    override val name = PretypeName("Int")
}

data object BooleanTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.boolType()
    override val name = PretypeName("Boolean")
}

data object CharTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.charType()
    override val name = PretypeName("Char")
}

data object StringTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.stringType()
    override val name = PretypeName("String")
}


data object IntArrayTypeEmbedding : ClassTypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.intArrayType()
    override val name =
        ScopedName(PackageScope(FqName.fromSegments(SpecialPackages.kotlin)), ClassKotlinName(listOf("IntArray")))
    override val isManual: Boolean = false
    override val uniquePredicateName: ScopedName = ScopedName(name.asScope(), PredicateName("unique"))

    context(ctx: TypeResolver)
    override fun uniquePredicate() = uniquePredicateNoContext()

    fun uniquePredicateNoContext(): Predicate {
        val aVar = Var(DispatchReceiverName, Type.Ref)
        val jVar = domainVar("j", Type.Int)
        val aExp = aVar.use()

        val typeCheck = RuntimeTypeDomain.isSubtype(
            RuntimeTypeDomain.typeOf(aExp),
            RuntimeTypeDomain.intArrayType()
        )

        val sizeApp = RuntimeTypeDomain.size(aExp)

        val quantifiedAcc = Exp.forall(jVar) { j ->
            assumption { (j ge 0.toExp()) and (j lt sizeApp) }
            val slotApp = simpleTrigger { RuntimeTypeDomain.slot(aExp, j) }
            Exp.Acc(
                Exp.FieldAccess(slotApp, ArrayCellDataFieldEmbedding.toViper()),
                PermExp.FullPerm()
            )
        }

        val body = listOf(typeCheck, quantifiedAcc).toConjunction()

        return Predicate(
            uniquePredicateName,
            listOf(aVar.decl()),
            body,
            includeInDumpPolicy = IncludeInDumpPolicy.ONLY_IN_FULL_DUMP,
        )
    }

    override fun uniquePredicateAccessInvariant(): TypeInvariantEmbedding =
        PredicateAccessTypeInvariantEmbedding(uniquePredicateName, PermExp.FullPerm())
}

fun PretypeEmbedding.asTypeEmbedding() = TypeEmbedding(this, TypeEmbeddingFlags(nullable = false))
