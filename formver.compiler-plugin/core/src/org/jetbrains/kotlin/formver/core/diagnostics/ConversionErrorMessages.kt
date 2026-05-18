/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

object ConversionErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("FormalVerificationConversion") { map ->
        map.put(
            ConversionErrors.PURITY_VIOLATION,
            "{0}",
            CommonRenderers.STRING,
        )
        map.put(
            ConversionErrors.ADT_VIOLATION,
            "{0}",
            CommonRenderers.STRING,
        )
        map.put(
            ConversionErrors.MINOR_INTERNAL_ERROR,
            "Formal verification non-fatal internal error: {0}",
            CommonRenderers.STRING,
        )
    }
}
