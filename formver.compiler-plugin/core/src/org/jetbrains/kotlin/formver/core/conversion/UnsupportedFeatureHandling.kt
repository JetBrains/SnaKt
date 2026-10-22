/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.common.UnsupportedFeatureBehaviour

/**
 * Reports a construct the user wrote that the plugin does not support. Internal invariant
 * violations use `SnaktInternalException` or `error` instead.
 *
 * In `THROW_EXCEPTION` mode this throws. In `ASSUME_UNREACHABLE` mode it reports a minor
 * internal error and returns [onUnreachable], so conversion continues past the construct.
 */
inline fun <T> ProgramConversionContext.handleUnsupportedFeature(
    source: KtSourceElement?,
    msg: String,
    onUnreachable: () -> T,
): T = when (config.behaviour) {
    UnsupportedFeatureBehaviour.THROW_EXCEPTION -> throw SnaktInternalException(source, msg)
    UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
        reportMinorInternalError(msg)
        onUnreachable()
    }
}
