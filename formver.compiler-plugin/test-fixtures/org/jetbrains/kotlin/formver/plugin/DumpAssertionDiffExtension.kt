package org.jetbrains.kotlin.formver.plugin

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher
import org.opentest4j.AssertionFailedError
import java.io.File

// Dumps expected vs actual content from golden-file assertion failures to
// /tmp/test-assertion-dump-*.txt. Inert unless registered via
// META-INF/services and enabled via junit-platform.properties autodetection;
// scripts/dump-test-diff.sh manages that registration transiently.
class DumpAssertionDiffExtension : TestWatcher {
    override fun testFailed(context: ExtensionContext, cause: Throwable) {
        val baseName = context.displayName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val assertions = collectAssertionErrors(cause)
        for ((index, error) in assertions.withIndex()) {
            val suffix = if (assertions.size > 1) "_$index" else ""
            val name = "$baseName$suffix"
            val expected = resolveValue(error.expected)
            val actual = resolveValue(error.actual)
            File("/tmp/test-assertion-dump-$name.txt").writeText(buildString {
                appendLine("=== EXPECTED ===")
                appendLine(expected)
                appendLine()
                appendLine("=== ACTUAL ===")
                appendLine(actual)
            })
            System.err.println(">>> Assertion diff dumped to /tmp/test-assertion-dump-$name.txt")
        }
    }

    private fun collectAssertionErrors(throwable: Throwable): List<AssertionFailedError> {
        if (throwable is AssertionFailedError && throwable.isExpectedDefined && throwable.isActualDefined) {
            return listOf(throwable)
        }
        // MultipleFailuresError (opentest4j, Assertions.assertAll) exposes getFailures();
        // Gradle's DefaultMultiCauseException exposes getCauses(). Both give a List<Throwable>.
        for (methodName in listOf("getFailures", "getCauses")) {
            try {
                val method = throwable.javaClass.getMethod(methodName)
                @Suppress("UNCHECKED_CAST")
                val children = method.invoke(throwable) as? List<Throwable>
                if (children != null && children.isNotEmpty()) {
                    return children.flatMap { collectAssertionErrors(it) }
                }
            } catch (_: Exception) {}
        }
        val fromSuppressed = throwable.suppressed.flatMap { collectAssertionErrors(it) }
        if (fromSuppressed.isNotEmpty()) return fromSuppressed
        val inner = throwable.cause
        if (inner != null && inner !== throwable) {
            return collectAssertionErrors(inner)
        }
        return emptyList()
    }

    private fun resolveValue(wrapper: org.opentest4j.ValueWrapper): String {
        val value = wrapper.value
        if (value != null && value !is String) {
            try {
                val m = value.javaClass.getMethod("getContentsAsString")
                return m.invoke(value) as String
            } catch (_: Exception) {}
        }
        val str = (value as? String) ?: wrapper.stringRepresentation
        // FileInfo.toString() embeds the path; read the file directly when we see it.
        val match = Regex("""FileInfo\[path='(.+?)',""").find(str)
        if (match != null) {
            val path = match.groupValues[1]
            try { return File(path).readText() } catch (_: Exception) {}
        }
        return str
    }
}
