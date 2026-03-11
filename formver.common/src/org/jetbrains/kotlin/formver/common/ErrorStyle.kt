/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

/**
 * Controls how Silicon verification failures are presented to the user.
 *
 * Configure via `formver { errorStyle("...") }` in Gradle, or via the
 * `-P plugin:org.jetbrains.kotlin.formver:error_style=...` command-line option.
 *
 * This setting has no effect on `INTERNAL_ERROR`, `PURITY_VIOLATION`, or `UNIQUENESS_VIOLATION`
 * diagnostics, which are always shown in their standard form.
 *
 * @see PluginConfiguration.errorStyle
 */
enum class ErrorStyle {
    /**
     * Translate Viper errors into user-friendly Kotlin diagnostics where possible; fall back to the
     * raw Viper/Silicon error message when no translation is available. This is the default.
     */
    USER_FRIENDLY,

    /** Always show the raw Viper/Silicon error message, without any translation. */
    ORIGINAL_VIPER,

    /**
     * Show both the user-friendly translation (when available) and the raw Viper/Silicon error.
     * Useful for debugging: you can see the high-level message and the underlying Silicon output together.
     */
    BOTH;

    companion object {
        @JvmStatic
        fun defaultBehaviour(): ErrorStyle = USER_FRIENDLY
    }
}
