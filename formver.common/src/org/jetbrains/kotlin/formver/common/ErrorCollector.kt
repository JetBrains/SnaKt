/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

import org.jetbrains.kotlin.KtSourceElement

/**
 * Accumulates plugin diagnostics during the conversion of a single function, to be reported
 * together once conversion completes.
 *
 * We currently are not consistent with what we report this way, vs through other channels.
 *
 * TODO: Replace this with some kind of more systematic approach to generating diagnostics.
 */
class ErrorCollector {
    private val minorErrors = mutableListOf<String>()
    private val purityErrors = mutableListOf<Pair<KtSourceElement, String>>()

    /** Records a non-fatal conversion error. These are later emitted as `MINOR_INTERNAL_ERROR` diagnostics. */
    fun addMinorError(error: String) {
        minorErrors.add(error)
    }

    /** Invokes [action] for each collected minor error message. */
    fun forEachMinorError(action: (String) -> Unit) {
        minorErrors.forEach(action)
    }

    /**
     * Records a purity violation at the given source position.
     * These are later emitted as `PURITY_VIOLATION` diagnostics.
     *
     * @param position The source element nearest to the violation.
     * @param msg A human-readable description of the violation.
     */
    fun addPurityError(position: KtSourceElement, msg: String) {
        purityErrors.add(Pair(position, msg))
    }

    /** Invokes [action] for each collected purity error, passing the source position and message. */
    fun forEachPurityError(action: (KtSourceElement, String) -> Unit) {
        purityErrors.forEach { (key, value) ->
            action(key, value)
        }
    }
}
