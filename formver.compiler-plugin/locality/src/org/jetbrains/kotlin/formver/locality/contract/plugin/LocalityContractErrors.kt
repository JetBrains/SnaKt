/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.contract.plugin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error3
import org.jetbrains.kotlin.fir.types.ConeKotlinType

object LocalityContractErrors : KtDiagnosticsContainer() {
    val LOCALITY_CONTRACT_MISMATCH by error3<PsiElement, String, LocalityContract?, LocalityContract?>()
    val CONTEXT_LOCALITY_CONTRACT_MISMATCH by error3<PsiElement, ConeKotlinType, LocalityContract?, LocalityContract?>()

    override fun getRendererFactory() = LocalityContractErrorMessages
}
