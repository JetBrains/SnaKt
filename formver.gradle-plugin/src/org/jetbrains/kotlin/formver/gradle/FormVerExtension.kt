/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.gradle


/**
 * Gradle DSL extension that configures the FormVer compiler plugin.
 *
 * Applied automatically by [FormVerGradleSubplugin] when the plugin is on the classpath.
 * Users configure it in their build script via the `formver { … }` block:
 *
 * ```kotlin
 * formver {
 *     logLevel("FULL_VIPER_DUMP")
 *     errorStyle("USER_FRIENDLY")
 *     conversionTargetsSelection("ALL_TARGETS")
 *     verificationTargetsSelection("ALL_TARGETS")
 *     dumpViperFiles(true)
 * }
 * ```
 *
 * Each setter stores its value in an internal backing field that
 * [FormVerGradleSubplugin.applyToCompilation] reads and converts into
 * [org.jetbrains.kotlin.gradle.plugin.SubpluginOption] entries passed to the Kotlin compiler.
 */
open class FormVerExtension {
    internal var myLogLevel: String? = null
    internal var myErrorStyle: String? = null
    internal var myUnsupportedFeatureBehaviour: String? = null
    internal var myConversionTargetsSelection: String? = null
    internal var myVerificationTargetsSelection: String? = null
    internal var myDumpViperFiles: String? = null

    /**
     * Sets the verbosity of the Viper program output emitted as compiler diagnostics.
     *
     * Accepted values match the `LogLevel` enum in the compiler plugin (e.g.
     * `"ONLY_WARNINGS"`, `"SHORT_VIPER_DUMP"`, `"SHORT_VIPER_DUMP_WITH_PREDICATES"`,
     * `"FULL_VIPER_DUMP"`).
     */
    open fun logLevel(logLevel: String) {
        myLogLevel = logLevel
    }

    /**
     * Sets the format used when reporting verifier errors back to the user.
     *
     * Accepted values match the `ErrorStyle` enum in the compiler plugin (e.g.
     * `"USER_FRIENDLY"`, `"ORIGINAL_VIPER"`, `"BOTH"`).
     */
    open fun errorStyle(style: String) {
        myErrorStyle = style
    }

    /**
     * Controls how the plugin responds when it encounters a Kotlin feature it does not
     * yet support.
     *
     * Accepted values match the `UnsupportedFeatureBehaviour` enum (e.g.
     * `"THROW_EXCEPTION"`, `"ASSUME_UNREACHABLE"`).
     */
    open fun unsupportedFeatureBehaviour(behaviour: String) {
        myUnsupportedFeatureBehaviour = behaviour
    }

    /**
     * Selects which functions are translated into Viper programs.
     *
     * Accepted values match the `TargetsSelection` enum (e.g.
     * `"ALL_TARGETS"`, `"TARGETS_WITH_CONTRACT"`, `"NO_TARGETS"`).
     */
    open fun conversionTargetsSelection(selection: String) {
        myConversionTargetsSelection = selection
    }

    /**
     * Selects which converted functions are subsequently verified by Silicon.
     *
     * Accepted values match the `TargetsSelection` enum (e.g.
     * `"ALL_TARGETS"`, `"TARGETS_WITH_CONTRACT"`, `"NO_TARGETS"`).
     */
    open fun verificationTargetsSelection(selection: String) {
        myVerificationTargetsSelection = selection
    }

    /**
     * When `true`, the generated Viper programs are written to `.formver/&#42;.vpr` inside
     * the project directory and their file URIs are emitted as compiler diagnostics.
     */
    open fun dumpViperFiles(dump: Boolean) {
        myDumpViperFiles = dump.toString()
    }
}