/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.diagnostics

import org.jetbrains.kotlin.KtSourceElement

/**
 * Surface for emitting conversion diagnostics.
 *
 * Kept separate from `ProgramConversionContext` so that subordinate code (purity checks, embedding
 * builders, etc.) can take only what it needs.
 */
interface ErrorCollectionContext {
    /** Report a purity violation at [source]. */
    fun reportPurityViolation(source: KtSourceElement?, msg: String)

    /** Report an ADT violation at [source]. */
    fun reportAdtViolation(source: KtSourceElement?, msg: String)

    /** Report a non-blocking internal-error notice; the source is supplied by the implementation. */
    fun reportMinorInternalError(msg: String)
}
