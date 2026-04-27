/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error3

object LocalityErrors : KtDiagnosticsContainer() {
    val LOCALITY_VIOLATION by error3<PsiElement, String, Locality, Locality>()
    val INVALID_LOCALITY_TYPE_TARGET by error0<PsiElement>()

    override fun getRendererFactory() = LocalityErrorMessages
}
