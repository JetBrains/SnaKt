/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.ProgramConversionContext
import org.jetbrains.kotlin.formver.core.conversion.PropertyName
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

fun ScopedNameBuilder.embedScope(id: ClassId) {
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

private fun CallableId.embedMemberPropertyNameBase(
    scopePolicy: MemberEmbeddingPolicy,
    withAction: (Name) -> KotlinName
): ScopedName {
    val id = classId ?: error("Embedding non-member property $callableName as a member.")
    return buildName {
        when (scopePolicy) {
            MemberEmbeddingPolicy.PUBLIC -> publicScope()
            MemberEmbeddingPolicy.PRIVATE -> {
                // When the field is private, we want to have the class scope as the parent.
                embedScope(id)
                privateScope()
            }
            MemberEmbeddingPolicy.UNSCOPED -> fakeScope()
        }
        withAction(callableName)
    }
}

fun CallableId.embedMemberPropertyName(isPrivate: Boolean) =
    embedMemberPropertyNameBase(alwaysScopedPolicy(isPrivate), ::PropertyKotlinName)

fun CallableId.embedMemberGetterName(isPrivate: Boolean) =
    embedMemberPropertyNameBase(alwaysScopedPolicy(isPrivate), ::GetterKotlinName)

fun CallableId.embedMemberSetterName(isPrivate: Boolean) =
    embedMemberPropertyNameBase(alwaysScopedPolicy(isPrivate), ::SetterKotlinName)

fun CallableId.embedMemberBackingFieldName(isPrivate: Boolean) =
    embedMemberPropertyNameBase(onlyPrivateScopedPolicy(isPrivate), ::BackingFieldKotlinName)

fun CallableId.embedUnscopedPropertyName(): SimpleKotlinName = SimpleKotlinName(callableName)
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
        callableId!!.embedMemberGetterName(Visibilities.isPrivate(visibility))
    }

fun FirPropertySymbol.embedSetterName(ctx: ProgramConversionContext): ScopedName =
    if (receiverParameterSymbol != null) {
        callableId!!.embedExtensionSetterName(
            ctx.embedFunctionPretype(
                setterSymbol ?: error("Embedding setter of read-only extension property.")
            )
        )
    } else {
        callableId!!.embedMemberSetterName(Visibilities.isPrivate(visibility))
    }

/**
 * Returns a pair that uniquely identifies the property.
 * The first element is the name of the class that contains the property, and the second is the name of the property itself.
 */
fun FirPropertySymbol.embedMemberPropertyName(): PropertyName {
    val className =
        callableId?.classId?.embedName() ?: throw SnaktInternalException(source, "Property is not part of a class")
    val propertyName = callableId!!.embedMemberPropertyName(Visibilities.isPrivate(this.visibility))
    return Pair(className, propertyName)
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
