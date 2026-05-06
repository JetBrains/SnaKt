/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent

abstract class CacheSessionComponent<K : Any, V, C>(
    session: FirSession
) : FirExtensionSessionComponent(session) {
    private val cache = session.firCachesFactory.createCache { key: K, context: C ->
        compute(key, context)
    }

    protected abstract fun compute(key: K, context: C): V

    fun getValue(key: K, context: C): V =
        cache.getValue(key, context)
}
