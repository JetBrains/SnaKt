# Testing swarm agent 451 diary

## Assignment

Issue [#451](https://github.com/JetBrains/SnaKt/issues/451) assigned boundary-value analysis of collections, arrays, and indexing. The assignment was recorded in `automationsInstructions/testing-swarm-agent-451.md` before investigation.

## Investigation and probes

- Read `AUTOMATIONS.md`, the testing guidance in `docs/agents-dev.md`, the existing List conversion and verification tests, and the collection specifications in `StdLibConverter.kt`.
- Searched open and closed GitHub issues before assessing defects. Issues [#252](https://github.com/JetBrains/SnaKt/issues/252), [#253](https://github.com/JetBrains/SnaKt/issues/253), and [#328](https://github.com/JetBrains/SnaKt/issues/328) already cover unsupported indexed writes and absent native-array size and bounds specifications.
- Added `collection_boundary_values.kt` with List reads at `-1`, `0`, `size - 1`, and `size`; the valid empty sublist range `[0, 0)`; a MutableList read at the new last index after one `add`; an IntArray guarded read at zero and unguarded read at `-1`; and an `Array<Int>` read at `size`.

## Commands and evidence

- `./agent-scripts/test.sh collection_boundary_values` initially could not start under the environment's Java 25.0.2 runtime. Installed a temporary Temurin 21 JDK and reran the command; conversion passed.
- `./agent-scripts/test.sh --verify collection_boundary_values` initially identified the missing `z3` executable. Installed the repository-documented Z3 4.8.7 release in a temporary directory and reran verification to observe the missing golden and diagnostic markers.
- `./agent-scripts/test.sh --update-goldens collection_boundary_values` rewrote one verification golden, generated the conversion golden, updated diagnostic markers, regenerated test registration, and passed test-data validation. The complete report contained exactly two verification diagnostics: the List index below zero and the List index equal to size.
- `./agent-scripts/test.sh --verify collection_boundary_values` then passed.
- `./agent-scripts/check-all.sh` completed the Gradle check and test-data check successfully. It exited 2 because `pre-commit` was unavailable. Installation was attempted first with user-site pip and then in a temporary virtual environment; the former was blocked by PEP 668 and the latter by the package proxy's HTTP 403 response.

## Classification and conclusions

- **Supported and verified:** guarded List reads at zero and `size - 1`, `emptyList<Int>().subList(0, 0)`, and the new last-index read after one MutableList addition.
- **Expected proof failure:** List read at `-1` reports the lower-bound diagnostic; List read at `size` reports the upper-bound diagnostic.
- **Unsupported semantics recorded as existing defects:** IntArray read at `-1` and generic Array read at `size` both verify because generated calls contain no bounds preconditions. The guarded IntArray control also verifies, but the generated `size` function has no non-negativity guarantee. This matches the already-reported array modeling gap in #253 and the focused open assignment #328, so no duplicate bug was filed.
- Indexed writes were not duplicated in testData because #252 already documents their internal-error behavior and the assignment required bounded probes.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=0c60b92c-0b90-4772-a227-069f72354386
