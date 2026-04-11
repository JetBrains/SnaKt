#!/usr/bin/env bash
# dump-test-diff.sh — Run a single failing test and dump the assertion diff.
#
# The Kotlin compiler test framework compares golden files (.fir.diag.txt, .kt
# with diagnostic markers) inside a forked test JVM. Gradle's cross-JVM
# serialization strips AssertionFailedError expected/actual values, so you
# never see the diff in normal test output.
#
# This script works around that by temporarily injecting a JUnit 5 TestWatcher
# extension that catches failures inside the test JVM and writes the diff to
# /tmp/test-assertion-dump-*.txt files.
#
# Usage:
#   ./scripts/dump-test-diff.sh "testIs_type_contract"
#   ./scripts/dump-test-diff.sh "testFull_viper_dump"
#
# After the run, look at /tmp/test-assertion-dump-*.txt for the expected vs
# actual content.

set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <test-method-name-pattern>"
    echo "Example: $0 'testIs_type_contract'"
    exit 1
fi

TEST_PATTERN="$1"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

EXTENSION_FILE="$ROOT_DIR/formver.compiler-plugin/test-fixtures/org/jetbrains/kotlin/formver/plugin/DumpAssertionDiffExtension.kt"
SERVICES_DIR="$ROOT_DIR/formver.compiler-plugin/test-fixtures/META-INF/services"
SERVICES_FILE="$SERVICES_DIR/org.junit.jupiter.api.extension.Extension"

# Clean up old dumps
rm -f /tmp/test-assertion-dump-*.txt

# Write the JUnit 5 extension
mkdir -p "$(dirname "$EXTENSION_FILE")"
cat > "$EXTENSION_FILE" << 'KOTLIN'
package org.jetbrains.kotlin.formver.plugin

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher
import org.opentest4j.AssertionFailedError
import java.io.File

class DumpAssertionDiffExtension : TestWatcher {
    override fun testFailed(context: ExtensionContext, cause: Throwable) {
        if (cause is AssertionFailedError && cause.isExpectedDefined && cause.isActualDefined) {
            val name = context.displayName.replace(Regex("[^a-zA-Z0-9_]"), "_")
            File("/tmp/test-assertion-dump-$name.txt").writeText(buildString {
                appendLine("=== EXPECTED ===")
                appendLine(cause.expected.stringRepresentation)
                appendLine()
                appendLine("=== ACTUAL ===")
                appendLine(cause.actual.stringRepresentation)
            })
            System.err.println(">>> Assertion diff dumped to /tmp/test-assertion-dump-$name.txt")
        }
    }
}
KOTLIN

# Register as auto-detected extension
mkdir -p "$SERVICES_DIR"
echo "org.jetbrains.kotlin.formver.plugin.DumpAssertionDiffExtension" > "$SERVICES_FILE"

cleanup() {
    rm -f "$EXTENSION_FILE"
    rm -f "$SERVICES_FILE"
    rmdir "$SERVICES_DIR" 2>/dev/null || true
    rmdir "$(dirname "$SERVICES_DIR")" 2>/dev/null || true
}
trap cleanup EXIT

echo "Running test: $TEST_PATTERN"
echo "Diff files will appear at /tmp/test-assertion-dump-*.txt"
echo

# --rerun-tasks forces recompilation of test-fixtures with the extension
cd "$ROOT_DIR"
./gradlew :formver.compiler-plugin:test \
    --tests "*$TEST_PATTERN*" \
    --rerun-tasks \
    --no-daemon \
    2>&1 || true

echo
echo "=== Assertion diffs ==="
for f in /tmp/test-assertion-dump-*.txt; do
    if [[ -f "$f" ]]; then
        echo
        echo "--- $(basename "$f") ---"
        cat "$f"
    fi
done

if ! ls /tmp/test-assertion-dump-*.txt &>/dev/null; then
    echo "(no diffs captured — test may have passed or failed with a non-assertion error)"
fi
