/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

typealias Locality = LocalityAttribute?

fun Locality.join(other: Locality): Locality =
    this?.union(other) ?: other
