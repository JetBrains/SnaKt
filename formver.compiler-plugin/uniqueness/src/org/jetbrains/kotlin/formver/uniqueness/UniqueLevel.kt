/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

/**
 * Ownership level of a value or access path in the uniqueness type system.
 *
 * Ordered from least to most permissive for lattice operations:
 * - [Unique] — the caller holds the sole reference; the value may be moved.
 * - [Shared] — the value has been shared; only borrowed access is permitted.
 * - [Top] — the value has already been moved (consumed) and may not be used again.
 *
 * The ordering is used by [UniquePathContext.subtreeLUB] and [UniquePathContext.pathToRootLUB]
 * to compute least upper bounds across an access-path trie.
 */
enum class UniqueLevel {
    Unique,
    Shared,
    Top,
}

/**
 * Indicates whether a function parameter is annotated as borrowed (`@Borrowed`) or is a
 * plain (consuming) parameter.
 *
 * - [Plain] — the callee takes ownership; the argument's uniqueness level is consumed.
 * - [Borrowed] — the callee only borrows the value; the caller retains ownership after the call.
 */
enum class BorrowingLevel {
    Plain,
    Borrowed,
}