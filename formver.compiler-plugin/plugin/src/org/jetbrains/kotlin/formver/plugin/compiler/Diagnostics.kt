/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*

context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A> info1(
    positioningStrategy: AbstractSourceElementPositioningStrategy = SourceElementPositioningStrategies.DEFAULT
): DiagnosticFactory1DelegateProvider<A> {
    return DiagnosticFactory1DelegateProvider(Severity.INFO, positioningStrategy, P::class, container)
}

context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A, B> info2(
    positioningStrategy: AbstractSourceElementPositioningStrategy = SourceElementPositioningStrategies.DEFAULT
): DiagnosticFactory2DelegateProvider<A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.INFO, positioningStrategy, P::class, container)
}