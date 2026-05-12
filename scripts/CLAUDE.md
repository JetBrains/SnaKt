# Scripts

`dump-test-diff.sh` debugs golden-file test failures by surfacing the expected/actual diff that Gradle's cross-JVM serialization normally strips.

```
./scripts/dump-test-diff.sh "testName"
```

Output lands in `$SNAKT_TEST_DUMP_DIR` (default `/tmp`):

- `test-assertion-diff-*.txt` — unified diff with source-position offsets (`/foo.kt:(23,31):`) collapsed to `:(_,_):` so unrelated offset shifts don't show as changes. Read this first.
- `test-assertion-dump-*.txt` — raw expected/actual content. Fall back to this when you need the original offsets.

Multiple failing assertions (e.g. both `.kt` and `.fir.diag.txt` broken) get separate numbered files.
