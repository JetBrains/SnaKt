/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common


/**
 * Controls what happens when the conversion engine encounters a Kotlin construct it cannot yet
 * translate to Viper.
 *
 * Configure via `formver { unsupportedFeatureBehaviour("...") }` in Gradle, or via the
 * `-P plugin:org.jetbrains.kotlin.formver:unsupported_feature_behaviour=...` command-line option.
 *
 * @see PluginConfiguration.behaviour
 */
enum class UnsupportedFeatureBehaviour {
    /**
     * Abort conversion of the affected function and report an `INTERNAL_ERROR` diagnostic. This is the default.
     * Compilation still succeeds; only verification of that function is skipped.
     */
    THROW_EXCEPTION,

    /**
     * Treat the unsupported construct as unreachable (`assume false`) and continue conversion.
     *
     * Useful for isolating other parts of a function when the unsupported feature lies on a
     * non-critical code path. With this setting the verifier may prove properties about the reachable
     * portions of the function even if some parts are stubbed out.
     */
    ASSUME_UNREACHABLE;

    companion object {
        @JvmStatic
        fun defaultBehaviour(): UnsupportedFeatureBehaviour {
            return THROW_EXCEPTION
        }
    }
}
