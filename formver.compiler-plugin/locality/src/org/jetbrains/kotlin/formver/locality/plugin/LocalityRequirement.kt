/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

enum class LocalityRequirement : SymbolicValue<LocalityRequirement> {
    RequireLocal, RequireGlobal;

    override fun accepts(other: LocalityRequirement): Boolean =
        this < other

    override fun union(other: LocalityRequirement): LocalityRequirement =
        minOf(this, other)
}
