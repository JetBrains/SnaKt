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
# It then post-processes each dump into /tmp/test-assertion-diff-*.txt — a
# unified diff with source-position prefixes (e.g. "/foo.kt:(23,31):")
# replaced by ":(_,_):" so methods that only had their offsets shifted by
# unrelated edits do not appear as spurious changes. The raw dumps are kept
# alongside in case the original offsets matter.
#
# Usage:
#   ./scripts/dump-test-diff.sh "testIs_type_contract"
#   ./scripts/dump-test-diff.sh "testFull_viper_dump"

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

rm -f /tmp/test-assertion-dump-*.txt /tmp/test-assertion-diff-*.txt

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
echo "Raw dumps at /tmp/test-assertion-dump-*.txt; normalized diffs at /tmp/test-assertion-diff-*.txt"
echo

# --rerun-tasks forces recompilation of test resources with the registration
cd "$ROOT_DIR"
./gradlew :formver.compiler-plugin:test \
    --tests "*$TEST_PATTERN*" \
    --rerun-tasks \
    --no-daemon \
    2>&1 || true

# Replace source-position offsets like ":(23,31):" with ":(_,_):" so methods
# that only shifted by edits to earlier code drop out of the diff. Restricted
# to lines starting with a "/path:" prefix to avoid false matches.
normalize_positions() {
    sed -E 's#^(/[^:]+):\([0-9]+,[0-9]+\):#\1:(_,_):#'
}

# Split a dump file at the "=== ACTUAL ===" marker into two files.
split_dump() {
    local dump="$1" expected_path="$2" actual_path="$3"
    awk -v exp_out="$expected_path" -v act_out="$actual_path" '
        /^=== EXPECTED ===$/ { side = "expected"; next }
        /^=== ACTUAL ===$/   { side = "actual";   next }
        side == "expected" { print > exp_out }
        side == "actual"   { print > act_out }
    ' "$dump"
}

shopt -s nullglob
for dump in /tmp/test-assertion-dump-*.txt; do
    base="$(basename "$dump" .txt)"
    base="${base#test-assertion-dump-}"
    exp_file="$(mktemp)"; act_file="$(mktemp)"
    exp_norm="$(mktemp)"; act_norm="$(mktemp)"
    split_dump "$dump" "$exp_file" "$act_file"
    normalize_positions < "$exp_file" > "$exp_norm"
    normalize_positions < "$act_file" > "$act_norm"
    diff -u --label "expected (positions normalized)" --label "actual (positions normalized)" \
        "$exp_norm" "$act_norm" > "/tmp/test-assertion-diff-$base.txt" || true
    rm -f "$exp_file" "$act_file" "$exp_norm" "$act_norm"
done

echo
echo "=== Normalized diffs (source-position offsets stripped) ==="
shown=0
for f in /tmp/test-assertion-diff-*.txt; do
    if [[ -s "$f" ]]; then
        echo
        echo "--- $(basename "$f") ---"
        cat "$f"
        shown=1
    fi
done

if [[ $shown -eq 0 ]]; then
    if compgen -G "/tmp/test-assertion-dump-*.txt" >/dev/null; then
        echo "(no real differences after normalizing positions — all changes were just offset shifts)"
        echo "Raw dumps remain at /tmp/test-assertion-dump-*.txt"
    else
        echo "(no diffs captured — test may have passed or failed with a non-assertion error)"
    fi
fi
