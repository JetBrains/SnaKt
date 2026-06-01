/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.isLocal
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

object TypeRefLocalityAttributeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    private fun FirElement.isValidLocalityHost(typeRef: FirResolvedTypeRef): Boolean =
        this is FirValueParameter ||
                this is FirReceiverParameter ||
                this is FirProperty && isLocal ||
                this is FirFunctionTypeParameter ||
                this is FirFunctionTypeRef && typeRef !== this.returnTypeRef ||
                source?.kind is KtFakeSourceElementKind.ImplicitTypeArgument

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        if (typeRef.coneType.attributes.locality == null) return

        val target = context.containingElements.dropLast(1).lastOrNull() ?: return

        if (target.isValidLocalityHost(typeRef)) return

        reporter.reportOn(typeRef.source, LocalityErrors.INVALID_LOCALITY_TYPE_TARGET)
    }
}
