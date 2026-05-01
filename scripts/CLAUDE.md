# Scripts

Use `dump-test-diff.sh` to debug golden file test failures. It runs a single test and dumps the expected vs actual content when an assertion fails, working around Gradle's cross-JVM serialization which strips diff details.

When both `.kt` and `.fir.diag.txt` golden files are broken, it captures each diff in a separate numbered file.

Usage:
```
./scripts/dump-test-diff.sh "testName"
```

Raw expected/actual content lands at `/tmp/test-assertion-dump-*.txt`. The script also writes a normalized unified diff to `/tmp/test-assertion-diff-*.txt` with source-position offsets (e.g. `/foo.kt:(23,31):`) stripped, so methods that only had their offsets shifted by unrelated edits do not show up as spurious changes — read the normalized diff first; fall back to the raw dump only if you need original offsets.
