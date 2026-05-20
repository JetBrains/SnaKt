/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType

private fun FirTypeRef.isImplicit(): Boolean =
    this is FirImplicitTypeRef ||
            source?.kind == KtFakeSourceElementKind.ImplicitTypeRef

context(context: CheckerContext)
fun FirVariable.resolveLocality(): Locality {
    if (!returnTypeRef.isImplicit()) return returnTypeRef.coneType.locality

    val typeLocality = returnTypeRef.coneType.locality

    return initializer?.resolveLocality() ?: typeLocality
}
