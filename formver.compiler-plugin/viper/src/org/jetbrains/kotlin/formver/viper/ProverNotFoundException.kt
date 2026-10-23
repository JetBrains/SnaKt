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

/** Returns a usable absolute Z3 path, leaving explicit prover configurations to Silicon. */
fun resolveZ3Executable(commandLineArgs: List<String>): String? {
    if (optionValue(commandLineArgs, "--z3Exe") != null) return null
    if ((optionValue(commandLineArgs, "--prover") ?: "Z3") != "Z3") return null
    val name = if (System.getProperty("os.name").lowercase().startsWith("windows")) "z3.exe" else "z3"
    val exe = System.getenv(Z3_EXE_ENV_VAR)?.let(::File)
        ?: System.getenv("PATH").orEmpty().split(File.pathSeparatorChar)
            .map { File(it, name) }
            .firstOrNull { it.isFile && it.canExecute() }
        ?: File(name)
    val hint = "Set the $Z3_EXE_ENV_VAR environment variable to the path of a z3 binary, or install Z3 " +
        "(https://github.com/Z3Prover/z3/releases) and add it to PATH."
    if (!exe.isFile) {
        throw ProverNotFoundException("Could not find the Z3 prover executable at '${exe.path}'. $hint")
    }
    if (!exe.canExecute()) {
        throw ProverNotFoundException("The Z3 prover at '${exe.path}' is not executable. $hint")
    }
    return exe.absolutePath
}
