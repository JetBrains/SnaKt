/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.ClassPropertyPair
import org.jetbrains.kotlin.formver.core.conversion.ProgramConversionContext
import org.jetbrains.kotlin.formver.core.conversion.PropertyKotlinName
import org.jetbrains.kotlin.formver.core.conversion.ScopeIndex
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/* This file contains classes to mangle names present in the Kotlin source.
 *
 * Name components should be separated by dollar signs.
 * If there is a risk of collision, add a prefix.
 */

fun ClassId.embedLocalName(): ClassKotlinName = ClassKotlinName(relativeClassName)

fun ScopedNameBuilder.embedScope(id: CallableId) {
    packageScope(id.packageName)
    id.classId?.let { classScope(it.embedLocalName()) }
}

fun ScopeBuilder.embedScope(id: ClassId) {
    packageScope(id.packageFqName)
    classScope(id.embedLocalName())
}

fun ClassId.embedName(): ScopedName = buildName {
    packageScope(packageFqName)
    embedLocalName()
}

fun CallableId.embedExtensionGetterName(type: FunctionTypeEmbedding): ScopedName = buildName {
    embedScope(this@embedExtensionGetterName)
    ExtensionGetterKotlinName(callableName, type)
}

fun CallableId.embedExtensionSetterName(type: FunctionTypeEmbedding): ScopedName = buildName {
    embedScope(this@embedExtensionSetterName)
    ExtensionSetterKotlinName(callableName, type)
}

private fun CallableId.embedMemberPropertyScope(
    scopePolicy: MemberEmbeddingPolicy,
): NameScope {
    val id = classId ?: error("Embedding non-member property $callableName as a member.")
    return buildScope {
        when (scopePolicy) {
            MemberEmbeddingPolicy.METHOD -> {
                publicScope()
            }

            MemberEmbeddingPolicy.FUNCTION -> {
                embedScope(id)
            }

            MemberEmbeddingPolicy.BACKING_FIELD -> {
                fakeScope()
            }

            MemberEmbeddingPolicy.PRIVATE_BACKING_FIELD -> {
                embedScope(id)
                privateScope()
            }
        }
    }
}

fun CallableId.embedMemberPropertyNameBase(
    scopePolicy: MemberEmbeddingPolicy,
    action: (Name) -> KotlinName
): ScopedName {
    val scope = embedMemberPropertyScope(scopePolicy)
    return ScopedName(scope, action(callableName))
}

fun CallableId.embedMemberPropertyName(scopePolicy: MemberEmbeddingPolicy): PropertyKotlinName {
    val scope = embedMemberPropertyScope(scopePolicy)
    return PropertyKotlinName(scope, callableName)
}

fun CallableId.embedMemberGetterName(scopePolicy: MemberEmbeddingPolicy) =
    embedMemberPropertyNameBase(scopePolicy, ::GetterKotlinName)

fun CallableId.embedMemberSetterName(scopePolicy: MemberEmbeddingPolicy) =
    embedMemberPropertyNameBase(scopePolicy, ::SetterKotlinName)

fun CallableId.embedMemberBackingFieldName(scopePolicy: MemberEmbeddingPolicy) =
    embedMemberPropertyNameBase(scopePolicy, ::BackingFieldKotlinName)

fun CallableId.embedFunctionName(type: FunctionTypeEmbedding): ScopedName = buildName {
    embedScope(this@embedFunctionName)
    FunctionKotlinName(callableName, type)
}

fun Name.embedScopedLocalName(scope: ScopeIndex) = buildName {
    when (scope) {
        is ScopeIndex.Indexed -> localScope(scope.index)
        // If we're in the context where creating locals is not permitted
        // an error would be reported down the stream (most likely, when we convert
        // ExpEmbedding to Viper)
        // For extra safety, we produce a name for such a variable that does not compile
        // TODO: make reporting more transparent here
        ScopeIndex.NoScope -> badScope()
    }
    SimpleKotlinName(this@embedScopedLocalName)
}

fun Name.embedParameterName() = buildName {
    parameterScope()
    SimpleKotlinName(this@embedParameterName)
}

fun FirValueParameterSymbol.embedName(): ScopedName = name.embedParameterName()

fun FirPropertySymbol.embedGetterName(ctx: ProgramConversionContext): ScopedName =
    if (receiverParameterSymbol != null) {
        callableId!!.embedExtensionGetterName(ctx.embedFunctionPretype(getterSymbol!!))
    } else {
        callableId!!.embedMemberGetterName(scopePolicy(this, ctx))
    }

fun FirPropertySymbol.embedSetterName(ctx: ProgramConversionContext): ScopedName =
    if (receiverParameterSymbol != null) {
        callableId!!.embedExtensionSetterName(
            ctx.embedFunctionPretype(
                setterSymbol ?: error("Embedding setter of read-only extension property.")
            )
        )
    } else {
        callableId!!.embedMemberSetterName(scopePolicy(this, ctx))
    }

/**
 * Returns a pair that uniquely identifies the property.
 * The first element is the name of the class that contains the property, and the second is the name of the property itself.
 */
fun FirPropertySymbol.embedMemberPropertyName(ctx: ProgramConversionContext): ClassPropertyPair {
    val callable = callableId
    val className =
        callable?.classId?.embedName() ?: throw SnaktInternalException(source, "Property is not part of a class")
    val propertyName = callable.embedMemberPropertyName(scopePolicy(this, ctx))
    return ClassPropertyPair(className, propertyName)
}


fun FirConstructorSymbol.embedName(ctx: ProgramConversionContext): ScopedName = buildName {
    embedScope(callableId)
    ConstructorKotlinName(ctx.embedFunctionPretype(this@embedName))
}

fun FirFunctionSymbol<*>.embedName(ctx: ProgramConversionContext): ScopedName = when (this) {
    is FirPropertyAccessorSymbol -> if (isGetter) propertySymbol.embedGetterName(ctx) else propertySymbol.embedSetterName(
        ctx
    )

    is FirConstructorSymbol -> embedName(ctx)
    else -> callableId.embedFunctionName(ctx.embedFunctionPretype(this))
}
