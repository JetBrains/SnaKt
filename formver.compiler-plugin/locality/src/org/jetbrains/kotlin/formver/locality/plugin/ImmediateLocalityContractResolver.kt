/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.functionTypeKind
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.valueParameterTypesIncludingReceiver

@OptIn(SymbolInternals::class, UnresolvedExpressionTypeAccess::class)
object ImmediateLocalityContractResolver {
    fun resolve(expression: FirExpression): LocalityContract =
        when (expression) {
            is FirAnonymousFunctionExpression ->
                expression.anonymousFunction.resolveLocalityContract()
            is FirCallableReferenceAccess ->
                expression.resolveCallableReferenceContract()
            is FirQualifiedAccessExpression ->
                expression.resolveQualifiedAccessTypeContract()
            else ->
                EmptyContract
        }

    private fun FirCallableReferenceAccess.resolveCallableReferenceContract(): LocalityContract {
        val callableSymbol = toResolvedCallableSymbol()
        val session = callableSymbol?.moduleData?.session
            ?: return EmptyContract
        val typeContract = resolveTypeContract(session)

        if (typeContract != EmptyContract) return typeContract

        return when (callableSymbol) {
            is FirFunctionSymbol<*> -> callableSymbol.fir.resolveLocalityContract()
            else -> EmptyContract
        }
    }

    private fun FirQualifiedAccessExpression.resolveQualifiedAccessTypeContract(): LocalityContract {
        val session = toResolvedCallableSymbol()?.moduleData?.session
            ?: return EmptyContract

        return resolveTypeContract(session)
    }

    private fun FirExpression.resolveTypeContract(session: FirSession): LocalityContract =
        coneTypeOrNull?.resolveLocalityContract(session) ?: EmptyContract

    private fun FirFunction.resolveLocalityContract(): LocalityContract =
        LocalityContract(
            buildList {
                contextParameters.mapTo(this) { parameter ->
                    parameter.returnTypeRef.resolveLocalityRequirement()
                }
                receiverParameter?.let { receiver ->
                    add(receiver.typeRef.resolveLocalityRequirement())
                }
                valueParameters.mapTo(this) { parameter ->
                    parameter.returnTypeRef.resolveLocalityRequirement()
                }
            }
        )

    private fun ConeKotlinType.resolveLocalityContract(session: FirSession): LocalityContract {
        val functionType = fullyExpandedType(session).lowerBoundIfFlexible() as? ConeClassLikeType
            ?: return EmptyContract

        if (functionType.functionTypeKind(session) == null) return EmptyContract

        return LocalityContract(
            functionType.valueParameterTypesIncludingReceiver(session)
                .map { it.resolveLocalityRequirement() }
        )
    }
}

fun FirExpression.resolveImmediateLocalityContract(): LocalityContract =
    ImmediateLocalityContractResolver.resolve(this)
