/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

object LocalityErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("FormalVerificationLocality") { map ->
        map.put(
            LocalityErrors.LOCALITY_VIOLATION,
            "{0} locality mismatch: expected {1}, actual {2}.",
            CommonRenderers.STRING,
            LocalityRenderer,
            LocalityRenderer,
        )
        map.put(
            LocalityErrors.INVALID_LOCALITY_TARGET,
            "Locality can only be specified on types of function parameters, extension receivers, or local variables.",
        )
    }
}
