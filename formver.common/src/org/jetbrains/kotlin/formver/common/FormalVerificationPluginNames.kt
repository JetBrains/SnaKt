/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

/**
 * String constants shared between the CLI processor, the Gradle plugin, and the compiler plugin
 * to identify the plugin and its configuration options by name.
 *
 * Using this object ensures that option names in the CLI argument parser, the Gradle DSL, and
 * the plugin registration all stay in sync. Any rename of a configuration option must be
 * reflected here, in the CLI parser, and in the Gradle extension simultaneously.
 */
object FormalVerificationPluginNames {
    /** The compiler plugin ID, used to register the plugin with `kotlinc`. */
    const val PLUGIN_ID = "org.jetbrains.kotlin.formver"

    /** Option name for [LogLevel] configuration. Gradle DSL: `logLevel(...)`. */
    const val LOG_LEVEL_OPTION_NAME = "log_level"

    /** Option name for [ErrorStyle] configuration. Gradle DSL: `errorStyle(...)`. */
    const val ERROR_STYLE_NAME = "error_style"

    /** Option name for [UnsupportedFeatureBehaviour] configuration. Gradle DSL: `unsupportedFeatureBehaviour(...)`. */
    const val UNSUPPORTED_FEATURE_BEHAVIOUR_OPTION_NAME = "unsupported_feature_behaviour"

    /** Option name for the conversion [TargetsSelection]. Gradle DSL: `conversionTargetsSelection(...)`. */
    const val CONVERSION_TARGETS_SELECTION_OPTION_NAME = "conversion_targets_selection"

    /** Option name for the verification [TargetsSelection]. Gradle DSL: `verificationTargetsSelection(...)`. */
    const val VERIFICATION_TARGETS_SELECTION_OPTION_NAME = "verification_targets_selection"

    /** Option name for enabling Viper file output. Gradle DSL: `dumpViperFiles(...)`. */
    const val DUMP_VIPER_FILES_OPTION_NAME = "dump_viper_files"

    /** Option name for passing the project root directory path. */
    const val PROJECT_DIR_OPTION_NAME = "project_dir"
}
