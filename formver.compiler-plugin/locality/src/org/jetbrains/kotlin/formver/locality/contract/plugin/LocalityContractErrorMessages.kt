/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.contract.plugin

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers

object LocalityContractErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap
            by KtDiagnosticFactoryToRendererMap("FormalVerificationLocality") { map ->
                map.put(
                    LocalityContractErrors.LOCALITY_CONTRACT_MISMATCH,
                    "{0} locality contract mismatch: expected ''{1}'', actual ''{2}''.",
                    CommonRenderers.STRING,
                    LocalityContractRenderer,
                    LocalityContractRenderer,
                )
                map.put(
                    LocalityContractErrors.CONTEXT_LOCALITY_CONTRACT_MISMATCH,
                    "Locality contract mismatch for context parameter of type ''{0}'': expected ''{1}'', actual ''{2}''.",
                    FirDiagnosticRenderers.RENDER_TYPE,
                    LocalityContractRenderer,
                    LocalityContractRenderer,
                )
            }
}
