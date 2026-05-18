/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.isLocal
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

object TypeLocalityAttributeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    private fun FirElement.isValidLocalityTarget(): Boolean =
        this is FirValueParameter ||
                this is FirReceiverParameter ||
                this is FirProperty && isLocal

    private fun CheckerContext.isValidLocalityTarget(): Boolean {
        val target = containingElements.dropLast(1).lastOrNull()

        return target?.isValidLocalityTarget() == true
    }

    private fun CheckerContext.isValidFunctionTypeArgumentTarget(typeRef: FirResolvedTypeRef): Boolean {
        if (containingElements.none { it.isValidLocalityTarget() }) return false

        return when (val target = containingElements.dropLast(1).lastOrNull()) {
            is FirFunctionTypeParameter ->
                true
            is FirFunctionTypeRef ->
                target.receiverTypeRef === typeRef || target.contextParameterTypeRefs.any { it === typeRef }
            else ->
                false
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        if (typeRef.coneType.attributes.locality == null) return

        if (context.isValidLocalityTarget() || context.isValidFunctionTypeArgumentTarget(typeRef)) return

        reporter.reportOn(typeRef.source, LocalityErrors.INVALID_LOCALITY_TYPE_TARGET)
    }
}
