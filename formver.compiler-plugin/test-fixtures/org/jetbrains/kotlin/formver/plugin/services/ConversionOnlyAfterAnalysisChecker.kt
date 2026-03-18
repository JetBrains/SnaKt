/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.services

import org.jetbrains.kotlin.diagnostics.AbstractKtDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.formver.plugin.compiler.PluginErrors
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.NEVER_VALIDATE
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * In conversion-only mode (`-Pformver.conversionOnly=true`), verification is
 * skipped so verification diagnostics (all warnings) are absent from the
 * actual output.  This causes two kinds of test failures in tests that
 * exercise verification:
 *
 * 1. Golden file (`.fir.diag.txt`) mismatch — the expected file has warning
 *    entries that the actual output doesn't.
 * 2. Inline marker mismatch — source files have `<!DIAGNOSTIC_NAME!>` markers
 *    for verification warnings that were not produced.
 *
 * This checker suppresses those failures by:
 * - For golden files: filtering warning-severity entries from expected text
 *   and re-comparing (line-based, structured format, no regex).
 * - For source markers: checking that all marker differences are exclusively
 *   verification diagnostics (parsed by name, no regex replacement).
 */
class ConversionOnlyAfterAnalysisChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {

    companion object {
        val conversionOnly: Boolean =
            System.getProperty("formver.conversionOnly")?.toBoolean() ?: false

        /**
         * Diagnostic names that are only produced by the verifier.
         * Extracted automatically: all WARNING-severity diagnostics in [PluginErrors].
         */
        val VERIFICATION_DIAGNOSTIC_NAMES: Set<String> by lazy {
            PluginErrors::class.java.declaredMethods
                .filter { it.parameterCount == 0 }
                .mapNotNull { method ->
                    val value = try { method.invoke(PluginErrors) } catch (_: Exception) { null }
                    if (value is AbstractKtDiagnosticFactory && value.severity == Severity.WARNING) {
                        value.name
                    } else null
                }
                .toSet()
        }

        /**
         * Regex matching the start of a diagnostic entry in a `.fir.diag.txt` golden file.
         * Format: `/filename.kt:(line,col): severity: message`
         */
        private val DIAGNOSTIC_ENTRY_START = Regex("""^/.+:\(\d+,\d+\): (info|warning|error): """)

        /**
         * Strips warning-severity diagnostic entries from golden file text.
         * Each entry starts with a path:position line and continues until the
         * next entry or EOF.  This is a structured line-based filter (no regex
         * on diagnostic content).
         */
        fun filterWarningsFromGoldenText(text: String): String {
            val lines = text.lines()
            val result = StringBuilder()
            var skipping = false

            for (line in lines) {
                val match = DIAGNOSTIC_ENTRY_START.find(line)
                if (match != null) {
                    skipping = match.groupValues[1] == "warning"
                }
                if (!skipping) {
                    if (result.isNotEmpty()) result.appendLine()
                    result.append(line)
                }
            }

            return result.toString()
        }

        /**
         * Checks whether ALL differences between expected and actual source text
         * are attributable to verification diagnostic markers.
         *
         * Rather than regex-replacing markers, this walks both texts in lockstep
         * and verifies that every divergence is a `<!VERIFICATION_DIAG!>` opening
         * tag or its corresponding `<!>` closing tag.
         */
        fun sourceMatchesWithoutVerificationMarkers(expected: String, actual: String): Boolean {
            // Strategy: scan the expected text character by character.
            // When we encounter a `<!NAME!>` marker, check if NAME is a verification
            // diagnostic.  If so, skip the opening tag and later skip its closing `<!>`.
            // Everything else must match the actual text exactly.
            var ei = 0  // index into expected
            var ai = 0  // index into actual
            var pendingClosingTags = 0  // verification closing tags to skip

            while (ei < expected.length) {
                // Check for marker opening: `<!`
                if (expected.startsWith("<!", ei)) {
                    // Check for closing tag `<!>`
                    if (expected.startsWith("<!>", ei)) {
                        if (pendingClosingTags > 0) {
                            // This closing tag belongs to a verification marker — skip it
                            pendingClosingTags--
                            ei += 3 // skip `<!>`
                            continue
                        }
                        // Non-verification closing tag — must appear in actual too
                    }

                    // Check for opening tag `<!NAME!>` or `<!NAME{...}!>`
                    val tagEnd = expected.indexOf("!>", ei + 2)
                    if (tagEnd != -1) {
                        val tagContent = expected.substring(ei + 2, tagEnd) // e.g. "CONDITIONAL_EFFECT_ERROR" or "NAME{params}"
                        val diagName = tagContent.substringBefore('{').substringBefore(',').trim()

                        if (diagName in VERIFICATION_DIAGNOSTIC_NAMES) {
                            // Skip this opening tag
                            pendingClosingTags++
                            ei = tagEnd + 2  // skip past `!>`
                            continue
                        }
                    }
                }

                // Characters must match
                if (ai >= actual.length || expected[ei] != actual[ai]) {
                    return false
                }
                ei++
                ai++
            }

            // Remaining characters (if any) must be only trailing whitespace
            val expectedRemaining = expected.substring(ei).isBlank()
            val actualRemaining = actual.substring(ai).isBlank()
            return expectedRemaining && actualRemaining && pendingClosingTags == 0
        }
    }

    override fun check(failedAssertions: List<WrappedException>) {
        // No-op: we handle everything in suppressIfNeeded.
    }

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val active = conversionOnly ||
            testServices.moduleStructure.modules.any { NEVER_VALIDATE in it.directives }
        if (!active) return failedAssertions
        return failedAssertions.filter { !shouldSuppress(it) }
    }

    private fun shouldSuppress(wrapped: WrappedException): Boolean {
        val cause = wrapped.cause ?: return false
        val message = cause.message ?: return false
        val (expected, actual) = extractExpectedActual(cause) ?: return false

        // Golden file mismatch — re-compare with warnings filtered out.
        if (message.contains(".fir.diag.txt")) {
            val filteredExpected = filterWarningsFromGoldenText(expected)
            val filteredActual = filterWarningsFromGoldenText(actual)
            return filteredExpected.trim() == filteredActual.trim()
        }

        // Source file marker mismatch — check if only verification markers differ.
        if (message.contains(".kt")) {
            return sourceMatchesWithoutVerificationMarkers(expected, actual)
        }

        return false
    }

    /**
     * Resolves a value (possibly a FileInfo) to its String content.
     */
    private fun resolveToString(value: Any?, wrapper: Any): String? {
        if (value is String) return value
        if (value != null) {
            // Try getContentsAsString (opentest4j FileInfo method)
            try {
                val m = value.javaClass.getMethod("getContentsAsString")
                val result = m.invoke(value)
                if (result is String) return result
            } catch (_: NoSuchMethodException) {}
            // Try reading from file path
            try {
                val getPath = value.javaClass.getMethod("getPath")
                val path = getPath.invoke(value)
                if (path is java.nio.file.Path) return path.toFile().readText()
                if (path is String) return java.io.File(path).readText()
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Extracts expected/actual strings from an assertion failure.
     * Supports opentest4j AssertionFailedError with ValueWrapper/FileInfo.
     */
    private fun extractExpectedActual(cause: Throwable): Pair<String, String>? {
        try {
            val getExpected = cause.javaClass.getMethod("getExpected")
            val getActual = cause.javaClass.getMethod("getActual")
            val expectedWrapper = getExpected.invoke(cause) ?: return null
            val actualWrapper = getActual.invoke(cause) ?: return null
            val getValue = expectedWrapper.javaClass.getMethod("getValue")

            val expectedObj = getValue.invoke(expectedWrapper)
            val actualObj = getValue.invoke(actualWrapper)

            if (expectedObj is String && actualObj is String) {
                return expectedObj to actualObj
            }

            val actualStr = resolveToString(actualObj, actualWrapper) ?: return null
            val expectedStr = resolveToString(expectedObj, expectedWrapper) ?: return null
            return expectedStr to actualStr
        } catch (_: Exception) {
            return null
        }
    }
}
