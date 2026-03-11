/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

/**
 * Aggregated configuration for a single SnaKt plugin invocation.
 *
 * Created by the CLI option parser ([org.jetbrains.kotlin.formver.cli]) and the Gradle plugin
 * ([org.jetbrains.kotlin.formver.gradle]) from the user-supplied settings, then passed to
 * `ViperPoweredDeclarationChecker` for use throughout the compilation session.
 *
 * The constructor enforces that [conversionSelection] is at least as broad as [verificationSelection]
 * — you cannot verify a function that has not been converted.
 *
 * @param logLevel Controls how much Viper output is emitted as compiler info diagnostics.
 * @param errorStyle Controls how Silicon verification failures are presented to the user.
 * @param behaviour What to do when an unsupported Kotlin construct is encountered during conversion.
 * @param conversionSelection Which functions are translated to Viper.
 * @param verificationSelection Which translated functions are submitted to Silicon for verification.
 * @param checkUniqueness Whether to run the standalone ownership/uniqueness checker.
 * @param dumpViperFiles Whether to write generated Viper programs to files on disk for inspection.
 * @param projectDir The project root directory; used to determine output paths when [dumpViperFiles] is `true`.
 */
class PluginConfiguration(
    val logLevel: LogLevel,
    val errorStyle: ErrorStyle,
    val behaviour: UnsupportedFeatureBehaviour,
    val conversionSelection: TargetsSelection,
    val verificationSelection: TargetsSelection,
    val checkUniqueness: Boolean,
    val dumpViperFiles: Boolean,
    val projectDir: String?,
) {
    init {
        require(conversionSelection >= verificationSelection) {
            "Conversion options may not be stricter than verification options; converting $conversionSelection but verifying $verificationSelection."
        }
    }

    override fun toString(): String =
        "PluginConfiguration(logLevel=$logLevel, errorStyle=$errorStyle, behaviour=$behaviour, " +
        "conversionSelection=$conversionSelection, verificationSelection=$verificationSelection, " +
        "checkUniqueness=$checkUniqueness, dumpViperFiles=$dumpViperFiles, projectDir=$projectDir)"
}
