/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

import java.io.File

private const val Z3_EXE_ENV_VAR = "Z3_EXE"

class ProverNotFoundException(override val message: String) : Exception(message)

/** The value of Silicon option [name] in [args], in either `--name value` or `--name=value` form. */
private fun optionValue(args: List<String>, name: String): String? {
    args.forEachIndexed { i, arg ->
        if (arg == name) return args.getOrNull(i + 1)
        if (arg.startsWith("$name=")) return arg.removePrefix("$name=")
    }
    return null
}

/**
 * Resolves the Z3 executable exactly as `viper.silicon.Config.z3Exe` does when no `--z3Exe`
 * argument is passed: [Z3_EXE_ENV_VAR] if set, otherwise the first entry of `PATH` that *contains*
 * a file or directory of the executable's name, otherwise that bare name.
 *
 * Any difference from Silicon's lookup would accept a prover Silicon rejects, or report a path
 * Silicon does not use.
 */
private fun resolveZ3Executable(getenv: (String) -> String?): File {
    getenv(Z3_EXE_ENV_VAR)?.let { return File(it) }
    val name = if (System.getProperty("os.name").lowercase().startsWith("windows")) "z3.exe" else "z3"
    val onPath = getenv("PATH").orEmpty().split(File.pathSeparatorChar)
        .map { File(it, name) }
        .firstOrNull { it.exists() }
    return onPath ?: File(name)
}

/**
 * Throws [ProverNotFoundException] with a user-facing diagnostic if Silicon, given [commandLineArgs],
 * would look up a Z3 executable from the environment and not find a usable one.
 *
 * An explicit `--z3Exe` or a `--prover` other than Z3 is left for Silicon to validate.
 */
fun checkProverIsUsable(commandLineArgs: List<String>, getenv: (String) -> String? = System::getenv) {
    if (optionValue(commandLineArgs, "--z3Exe") != null) return
    if ((optionValue(commandLineArgs, "--prover") ?: "Z3") != "Z3") return
    val exe = resolveZ3Executable(getenv)
    val hint = "Set the $Z3_EXE_ENV_VAR environment variable to the path of a z3 binary, or install Z3 " +
        "(https://github.com/Z3Prover/z3/releases) and add it to PATH."
    if (!exe.isFile) {
        throw ProverNotFoundException("Could not find the Z3 prover executable at '${exe.path}'. $hint")
    }
    if (!exe.canExecute()) {
        throw ProverNotFoundException("The Z3 prover at '${exe.path}' is not executable. $hint")
    }
}
