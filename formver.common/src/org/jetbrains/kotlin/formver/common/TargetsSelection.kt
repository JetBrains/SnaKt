/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

/**
 * Controls which Kotlin functions are included in the conversion or verification stage.
 *
 * Used for both [PluginConfiguration.conversionSelection] and [PluginConfiguration.verificationSelection].
 * The conversion selection must be at least as broad as the verification selection — you cannot
 * verify a function that has not been converted.
 *
 * Per-function annotations (`@AlwaysVerify`, `@NeverVerify`, `@NeverConvert`) always override
 * the project-level selection.
 *
 * Configure via `formver { conversionTargetsSelection("...") }` or
 * `formver { verificationTargetsSelection("...") }` in Gradle.
 *
 * @see PluginConfiguration.conversionSelection
 * @see PluginConfiguration.verificationSelection
 */
enum class TargetsSelection {
    /** Exclude all functions; effectively disables the stage. */
    NO_TARGETS,

    /**
     * Include only functions that carry a Kotlin `contract { }` block. This is the default.
     * Per-function overrides (`@AlwaysVerify`, `@NeverVerify`) still apply.
     */
    TARGETS_WITH_CONTRACT,

    /** Include every function in the module. */
    ALL_TARGETS;

    companion object {
        @JvmStatic
        fun defaultBehaviour(): TargetsSelection {
            return TARGETS_WITH_CONTRACT
        }
    }
}
