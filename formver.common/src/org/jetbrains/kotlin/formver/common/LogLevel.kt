/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

/**
 * Controls how much Viper output is emitted as compiler info diagnostics during a build.
 *
 * Configure via `formver { logLevel("...") }` in Gradle, or via the
 * `-P plugin:org.jetbrains.kotlin.formver:log_level=...` command-line option.
 *
 * Info messages are only visible when Gradle is run with `--info`.
 *
 * @see PluginConfiguration.logLevel
 */
enum class LogLevel {
    /** Emit no Viper output; only warnings and errors are shown. This is the default. */
    ONLY_WARNINGS,

    /** Emit the generated Viper program for each converted function, excluding predicate definitions. */
    SHORT_VIPER_DUMP,

    /** Emit the generated Viper program for each converted function, including predicate definitions. */
    SHORT_VIPER_DUMP_WITH_PREDICATES,

    /** Emit the complete Viper program including all declarations and predicate definitions. */
    FULL_VIPER_DUMP;

    companion object {
        @JvmStatic
        fun defaultLogLevel(): LogLevel {
            return ONLY_WARNINGS
        }
    }
}
