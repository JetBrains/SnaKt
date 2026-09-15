# Testing swarm agent 458 diary

## Assignment

GitHub issue #458 assigns collections, arrays, and indexing probes using fault
injection and failure propagation. Required coverage includes `List`, mutable
collections where supported, `Array` and primitive arrays, size facts, get/set,
empty and singleton cases, and bounds obligations, with positive and negative or
boundary controls.

## Actions

- Read `AUTOMATIONS.md` before taking any other repository action.
- Confirmed the checkout started on `implementing-air-automations` at `9bac7b3` with
  a clean worktree.
- Inspected existing automation instruction and diary names, recent source-branch
  history, and remote testing branch names.
- Created branch `test/swarm-458-collections-indexing` from
  `implementing-air-automations`.
- Recorded this assignment in
  `automationsInstructions/testing-swarm-agent-458.md` before exploring the
  implementation or tests.
- Used semantic code search and a read-only context explorer to locate existing
  collection/indexing conversion and golden-test conventions.
- Found existing focused coverage for `List` and `MutableList`, including safe
  guarded access and proof failures for empty, negative, and upper-bound access.
  `StdLibConverter` models `List.get`, `MutableList.add`, and collection size,
  but contains no matching `Array`, primitive-array, or indexed `set` contracts.
- Added an initial focused probe file with guarded positive controls and bounded
  negative/upper-bound reads for lists, object arrays, primitive arrays, and an
  object-array write. The expected diagnostics will be added only after observing
  the conversion and verification results.
- The first harness attempt failed before tests because the environment's Java 25
  runtime is unsupported by the build. Downloaded Temurin 21.0.12.1 to `/tmp` and
  reran with that compatible runtime.
- Fast conversion classified object-array indexed assignment as an `INTERNAL_ERROR`
  attributed to the assignment expression. All list reads, object-array reads, and
  primitive-array reads converted and emitted Viper text. Recorded only the observed
  internal-error marker, leaving verification outcomes to the full pipeline.
- Rerunning the array-assignment diagnostic changed the FIR object's rendered identity
  in the internal-error details (`FirUnitExpression@59d09ff3` became
  `FirUnitExpression@7bc6b117`). A golden containing it would be nondeterministic, so
  removed the assignment from the stable fixture while retaining this exact bounded
  reproducer here:

  ```kotlin
  @AlwaysVerify
  fun arraySetUpperBound(items: Array<Int>) {
      items[items.size] = 1
  }
  ```
- Installed the repository-pinned Z3 4.8.7 under `/tmp`, set `Z3_EXE`, and
  restarted Gradle so full verification could run.
- Full verification produced exactly three expected proof failures, all correctly
  source-attributed as `POSSIBLE_INDEX_OUT_OF_BOUND`: list index `-1`, list index
  `size`, and mutable-list index `size` after `add`.
- The guarded object-array and primitive-array controls were reported as verified,
  but so were object-array index `size` and primitive-array index `-1`. Their emitted
  `get` methods require only receiver/index types and contain no bounds obligations.
  Classified these unsafe successes as a confirmed verification-model defect.
- Searched open and closed GitHub issues for array bounds, array indexing, primitive
  arrays, and indexed writes. The unsafe read behavior was already reported in
  closed issue #253 (including an explicit comment about missing `get` bounds), and
  indexed writes were already reported in closed issue #252. Issue #328 is a current
  testing assignment for array indexing. Per the no-duplicate rule, opened no bug.
- Probed explicit empty and singleton construction. `emptyArray<Int>()[0]` converted
  and was admitted to the stable fixture. Both `arrayOf(1)[0]` and
  `intArrayOf(1)[0]` produced `INTERNAL_ERROR` on the literal vararg element during
  conversion, so singleton array construction is classified as an internal
  conversion error rather than supported verification. Removed those two internal
  errors from the stable golden because the assignment already demonstrated that
  internal FIR renderings can contain nondeterministic identities.
- Ran `--update-goldens` after settling the fixture and read its full report. It
  records only the three intended list proof failures; array upper-bound, empty-array
  index zero, and primitive-array negative-index probes produce no verification
  diagnostics. Confirmed the generated array `get` contracts lack both lower- and
  upper-bound requirements.
- Final focused `./agent-scripts/test.sh --verify collection_array_faults`: 1 test
  passed, 0 failed.
- Ran `./agent-scripts/check-all.sh`: Gradle `check` and testData checks passed, but
  the wrapper returned exit 2 because `pre-commit` was absent. Installed the official
  pre-commit 4.6.2 zipapp and reran: Gradle and testData passed again, while the
  pre-commit hook environment failed because the runner proxy returned HTTP 403 for
  the required PyPI `setuptools` download.
- Ran the relevant hooks directly after that infrastructure failure: upstream
  `end-of-file-fixer` over every changed file (it normalized the instruction file),
  `agent-scripts/tests/run.sh` (all assertions passed), `git diff --check` (passed),
  and `check-testdata.sh` via both full-check attempts (passed).
