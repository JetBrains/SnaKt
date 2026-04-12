# Scripts

Use `dump-test-diff.sh` to debug golden file test failures. It runs a single test and dumps the expected vs actual content when an assertion fails, working around Gradle's cross-JVM serialization which strips diff details.

When both `.kt` and `.fir.diag.txt` golden files are broken, it captures each diff in a separate numbered file.

Usage:
```
./scripts/dump-test-diff.sh "testName"
```

Output appears at `/tmp/test-assertion-dump-*.txt`.
