/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

import org.jetbrains.kotlin.KtSourceElement

/** Collector for some plugin errors.
 *
 * We currently are not consistent with what we report this way, vs through other channels.
 *
 * TODO: Replace this with some kind of more systematic approach to generating diagnostics.
 */
enum class AdtErrorKind { INVALID_TARGET, INVALID_USAGE }

class ErrorCollector {
    private val minorErrors = mutableListOf<String>()
    private val purityErrors = mutableListOf<Pair<KtSourceElement?, String>>()
    private val adtErrors = mutableListOf<Triple<AdtErrorKind, KtSourceElement?, String>>()

    fun addMinorError(error: String) {
        minorErrors.add(error)
    }

    fun forEachMinorError(action: (String) -> Unit) {
        minorErrors.forEach(action)
    }

    fun addPurityError(position: KtSourceElement?, msg: String) {
        purityErrors.add(Pair(position, msg))
    }

    fun forEachPurityError(action: (KtSourceElement?, String) -> Unit) {
        purityErrors.forEach { (key, value) ->
            action(key, value)
        }
    }

    fun collectedPurityError() = purityErrors.isNotEmpty()

    fun addAdtError(kind: AdtErrorKind, position: KtSourceElement?, msg: String) {
        adtErrors.add(Triple(kind, position, msg))
    }

    fun forEachAdtError(action: (AdtErrorKind, KtSourceElement?, String) -> Unit) {
        adtErrors.forEach { (kind, pos, msg) ->
            action(kind, pos, msg)
        }
    }

    fun collectedAdtError() = adtErrors.isNotEmpty()
    fun collectedAdtErrorOfKind(kind: AdtErrorKind) = adtErrors.any { it.first == kind }
}
