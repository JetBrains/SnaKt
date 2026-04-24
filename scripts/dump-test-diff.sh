#!/usr/bin/env bash
# dump-test-diff.sh — Run a single failing test and dump the assertion diff.
#
# The Kotlin compiler test framework compares golden files (.fir.diag.txt, .kt
# with diagnostic markers) inside a forked test JVM. Gradle's cross-JVM
# serialization strips AssertionFailedError expected/actual values, so you
# never see the diff in normal test output.
#
# This script works around that by temporarily registering a JUnit 5
# TestWatcher extension (DumpAssertionDiffExtension, in test-fixtures) that
# catches failures inside the test JVM and writes the diff to
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

SERVICES_DIR="$ROOT_DIR/formver.compiler-plugin/testData/META-INF/services"
SERVICES_FILE="$SERVICES_DIR/org.junit.jupiter.api.extension.Extension"
PLATFORM_PROPS="$ROOT_DIR/formver.compiler-plugin/testData/junit-platform.properties"

rm -f /tmp/test-assertion-dump-*.txt

# Register the extension via auto-detection. The DumpAssertionDiffExtension
# class itself lives in test-fixtures and is inert unless both of these files
# are present at test time.
mkdir -p "$SERVICES_DIR"
echo "org.jetbrains.kotlin.formver.plugin.DumpAssertionDiffExtension" > "$SERVICES_FILE"
echo "junit.jupiter.extensions.autodetection.enabled=true" > "$PLATFORM_PROPS"

cleanup() {
    rm -f "$SERVICES_FILE"
    rm -f "$PLATFORM_PROPS"
    rmdir "$SERVICES_DIR" 2>/dev/null || true
    rmdir "$(dirname "$SERVICES_DIR")" 2>/dev/null || true
    # Also clean up the build copy so it doesn't persist across runs
    rm -f "$ROOT_DIR/formver.compiler-plugin/build/resources/test/junit-platform.properties"
    rm -rf "$ROOT_DIR/formver.compiler-plugin/build/resources/test/META-INF/services"
}
trap cleanup EXIT

echo "Running test: $TEST_PATTERN"
echo "Diff files will appear at /tmp/test-assertion-dump-*.txt"
echo

# --rerun-tasks forces recompilation of test resources with the registration
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
