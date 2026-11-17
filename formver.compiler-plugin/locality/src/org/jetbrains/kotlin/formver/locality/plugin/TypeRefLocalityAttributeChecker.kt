/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.isLocal
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.formver.type.plugin.AttributeTargetJudgment
import org.jetbrains.kotlin.formver.type.plugin.TypeRefAttributeChecker

private fun FirTypeRef.removeResolved(): FirTypeRef =
    when (this) {
        is FirResolvedTypeRef -> delegatedTypeRef?.removeResolved() ?: this
        else -> this
    }

private object LocalityAttributeTargetJudgment : AttributeTargetJudgment {
    private fun isValidLocalityContractTarget(parents: List<FirElement>): Boolean {
        val currentElement = parents.getOrNull(0) ?: return false
        val containerElement = parents.getOrNull(1) ?: return false

        return when (containerElement) {
            is FirFunctionTypeParameter -> true
            is FirTypeRef -> {
                val functionTypeRef = containerElement.removeResolved() as? FirFunctionTypeRef
                    ?: return false

                currentElement == functionTypeRef.receiverTypeRef
                        || currentElement in functionTypeRef.contextParameterTypeRefs
            }
            else -> false
        }
    }

    private fun isValidLocalityTypeTarget(parents: List<FirElement>): Boolean {
        val targetElement = parents.getOrNull(1) ?: return false

        return when(targetElement) {
            is FirValueParameter, is FirReceiverParameter -> true
            is FirProperty -> targetElement.isLocal
            else -> targetElement.source?.kind is KtFakeSourceElementKind.ImplicitTypeArgument
        }
    }

    context(context: CheckerContext)
    override fun accepts(parents: List<FirElement>): Boolean =
        isValidLocalityTypeTarget(parents) || isValidLocalityContractTarget(parents)
}

val TypeRefLocalityAttributeChecker = TypeRefAttributeChecker(
    kind = MppCheckerKind.Common,
    attributeClass = LocalityAttribute::class,
    attributeTargetJudgment = LocalityAttributeTargetJudgment,
    diagnosticFactory = LocalityErrors.INVALID_LOCALITY_TYPE_TARGET,
)
