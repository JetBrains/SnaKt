/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries

/**
 * Manages temporary Viper dump files for one compilation session.
 *
 * Instantiated once per compilation in [PluginAdditionalCheckers]. On construction, the
 * `.formver/` directory inside [projectDir] is cleared and recreated, so every compilation
 * starts with a clean slate. Each call to [writeViperDump] writes one `.vpr` file there
 * and returns its `file://` URI.
 *
 * Add `.formver/` to the project's `.gitignore` to avoid accidental commits.
 */
class ViperDumpFileManager(projectDir: Path) {
    private val runDir: Path = run {
        val dir = projectDir.resolve(".formver")
        cleanUpPreviousRun(dir)
        Files.createDirectories(dir)
        dir
    }

    /**
     * Deletes all entries inside [dir] if it already exists, leaving the directory itself
     * in place for [Files.createDirectories] to use.  This gives each compilation a clean slate
     * without needing to recreate the parent path.
     */
    @OptIn(ExperimentalPathApi::class)
    private fun cleanUpPreviousRun(dir: Path) {
        if (Files.exists(dir)) {
            dir.listDirectoryEntries().forEach { it.deleteRecursively() }
        }
    }

    /**
     * Writes [content] to a file named `<declarationName>.vpr` inside the run directory.
     * If multiple declarations share the same name (e.g. overloads), a numeric suffix is
     * appended to avoid overwrites.
     *
     * @return a `file://` URI pointing to the written file.
     */
    fun writeViperDump(declarationName: String, content: String): URI {
        val safeName = declarationName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val file = resolveUniquePath(runDir, safeName)
        Files.writeString(file, content)
        return file.toUri()
    }

    /**
     * Returns a [Path] inside [dir] that does not yet exist.
     *
     * Tries `<baseName>.vpr` first; if that file already exists it tries
     * `<baseName>_1.vpr`, `<baseName>_2.vpr`, and so on until a free slot is found.
     */
    private fun resolveUniquePath(dir: Path, baseName: String): Path {
        val candidate = dir.resolve("$baseName.vpr")
        if (!Files.exists(candidate)) return candidate
        var counter = 1
        while (true) {
            val numbered = dir.resolve("${baseName}_$counter.vpr")
            if (!Files.exists(numbered)) return numbered
            counter++
        }
    }
}
