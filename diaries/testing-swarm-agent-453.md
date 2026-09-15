# Testing swarm agent 453 diary

## Assignment

Test collections, arrays, and indexing using contract mutations and positive/negative controls from GitHub issue #453. Work from `implementing-air-automations`, preserve golden observations deliberately, report only confirmed and previously unreported bugs, and deliver through a pull request to the source branch.

## Initial inspection

- Read `AUTOMATIONS.md` before repository investigation and recorded the assignment in `automationsInstructions/testing-swarm-agent-453.md`.
- Confirmed the checkout started on `implementing-air-automations` at `9bac7b3`, then created `automation/testing-swarm-453`.
- Used semantic search to locate collection contracts and nearby tests.
- Existing implementation models `Collection.size`, `Collection.isEmpty`, `List.get`, `List.subList`, `MutableList.add`, and `emptyList`. `List.get` requires `0 <= index < size`; `add` increases size by one.
- Existing tests already cover empty-list access, unguarded last access, one mutation after `add`, sub-list bounds, guarded list access, and custom list access. No array test data or array-specific standard-library contracts were found.

## Planned probes

- Mutable list: add one element, then compare index `0` (positive) with index `size` (negative boundary).
- Read-only list: guard with `isEmpty`, then compare `size - 1` (positive) with `size` (negative boundary).
- Object and primitive arrays: use the same guarded `0` versus `size` mutation to determine whether array size and indexing contracts are supported.

## Commands and environment

- `./agent-scripts/test.sh collection_index_mutations` initially failed before tests because the active JBR 25.0.2 runtime was unsupported by the build.
- Ubuntu package installation was unavailable because the configured mirrors could not resolve. Installed Temurin 21.0.12.1 under `/tmp/snakt-jdk21` from the official Adoptium GitHub release and reran with `JAVA_HOME` and `PATH` scoped to each command.
- `./agent-scripts/test.sh --verify collection_index_mutations` then reached Silicon but reported a harness failure because `z3` was absent.
- Installed the project-documented Z3 4.8.7 binary under `/tmp/snakt-z3-4.8.7` from the official Z3 GitHub release and reran with `Z3_EXE` scoped to each command.
- Ran the fast conversion loop separately for `collection_index_mutations` and `array_index_probes` while developing.
- Ran `./agent-scripts/test.sh --update-goldens collection_index_mutations`, read the complete report, and accepted exactly two expected list bounds diagnostics.
- Ran `./agent-scripts/test.sh --update-goldens array_index_probes`, read the complete report, and accepted the observed array conversion and verification diagnostics after splitting empty and singleton factories into minimal probes.
- Ran `./agent-scripts/test.sh --verify collection_index_mutations` and `./agent-scripts/test.sh --verify array_index_probes`; both passed with their final goldens.

## Evidence and classification

- `MutableList.add` followed by `xs[0]`: **supported and verified**. The generated `add` postcondition establishes that size increases by one, which discharges the `0 < size` access obligation.
- The single mutation to `xs[xs.size]`: **expected proof failure** with `POSSIBLE_INDEX_OUT_OF_BOUND` for the greater-than-size obligation.
- A `List` guarded by `isEmpty`, indexed at `size - 1`: **supported and verified**.
- The single mutation from `size - 1` to `size`: **expected proof failure** with the same intended bounds obligation.
- Guarded `Array<Int>` and `IntArray` reads at both `0` and `size`: **conversion supported, bounds verification unsupported**. The generated `get`, `size`, and `isEmpty` declarations have type contracts only. Consequently, even the definitely invalid index-equals-size controls produce no proof failure; these successes are not evidence of index safety.
- `Array<Int>` and `IntArray` assignments at both `0` and `size`: **internal error** during conversion (`FirUnitExpression` not implemented), independent of bounds.
- `emptyArray<Int>().size == 0`: **expected proof failure due to missing factory size facts**. The object-array assertion is reported explicitly; the primitive-array conversion has the same absence of a size postcondition.
- `arrayOf(1)` and `intArrayOf(1)`: **internal error** during conversion because general vararg arguments are unsupported. Empty factory calls convert, while singleton calls provide the mutation control.

## Existing issue search and conclusion

Searched open and closed GitHub issues for `array set INTERNAL_ERROR`, `Array indexing`, `IntArray`, and `array bounds`. The observed indexed-write failure is already documented by closed issue #252, and missing native-array size facts are already documented by closed issue #253. Open issue #328 is the prior swarm assignment for array indexing obligations. No new bug issue was filed because these results reproduce known capability gaps rather than a previously unreported defect.

## Final verification

- The first full `./agent-scripts/check-all.sh` run exposed nondeterministic object identity suffixes in goldenized internal-error details from the temporary set and singleton probes. Removed those unstable cases from committed test data while retaining their commands, classifications, and issue correlation in this diary.
- Reran `./agent-scripts/test.sh --update-goldens array_index_probes`, reviewed the complete report, and retained only deterministic conversion and verification observations.
- Reran `./agent-scripts/check-all.sh`: Gradle `check` passed, including all 144 compiler-plugin tests; `check-testdata.sh` passed. The wrapper exited 2 only because `pre-commit` was unavailable.
- Attempted to install `pre-commit` in an isolated virtual environment with both `pip` and `uv`; the environment proxy returned 403 for `files.pythonhosted.org`, and Ubuntu mirrors were also unavailable.
- Executed every configured hook directly: the end-of-file condition passed for all added files, `agent-scripts/check-testdata.sh` passed, and `agent-scripts/tests/run.sh` passed all assertions. `git diff --check` also passed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=82bb4133-2deb-4a04-bbff-93a44b6e7259
