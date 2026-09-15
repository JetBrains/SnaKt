# Testing swarm agent 456 diary

## Assignment

Test pairwise interactions among Kotlin collections, arrays, indexing, size facts, mutation where supported, empty and singleton cases, and bounds obligations. Add focused golden tests with positive and negative or boundary controls, classify outcomes, and report only confirmed previously unreported defects.

## Actions and evidence

- Read `AUTOMATIONS.md` before repository inspection.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-456.md`.
- Confirmed the checkout started on `implementing-air-automations` at `9bac7b3` with no pre-existing worktree changes.
- Confirmed issue 456 carries `swarmTesting` and requires final `swarmTestingDone` labeling.
- Inspected recent pull requests targeting `implementing-air-automations`; chose branch `test/issue-456-collection-array-pairs` to match the dominant `test/issue-<number>-<topic>` convention.

## Probe plan

- Pair list size guards with indexed reads, including a statically empty boundary.
- Pair `MutableList.add` with reads at the old size and at the new size.
- Compare reference-array size/read combinations against standalone `Array.size` and `IntArray.size` controls.
- Exercise indexed write separately to distinguish unsupported mutation from read behavior.

## Conclusions

- `guardedListRead`: **supported and verified**. The list size invariant and `get` precondition discharge the last-index access after a nonempty guard.
- `emptyListRead`: **expected proof failure**. Verification reports `POSSIBLE_INDEX_OUT_OF_BOUND` for index zero on `emptyList`.
- `singletonListRead`: **expected proof failure caused by an unsupported relational model**. `listOf(7)` converts, but its contract establishes only nonnegative size rather than size one, so the valid index-zero read cannot be proved. This matches the known generic `listOf` behavior explored by issue #329 rather than a new defect.
- `appendThenRead`: **supported and verified**. `MutableList.add` establishes `size == old(size) + 1`, making the old size a valid new index.
- `appendThenReadPastEnd`: **expected proof failure**. Reading at the updated size reports `POSSIBLE_INDEX_OUT_OF_BOUND`.
- `referenceArraySize`, `guardedReferenceArrayRead`, and `primitiveArraySize`: **supported and verified**, but the generated native-array contracts contain no size invariants or read bounds obligations.
- `emptyReferenceArrayRead`: incorrectly **supported and verified**. `emptyArray` has no size-zero postcondition and `Array.get` has no bounds precondition, so the impossible read verifies. Opened #482 with a minimal reproducer, expected/observed behavior, environment, and successful controls; applied `swarmTestingBug`.
- Temporary `IntArray` indexed-write probe: **internal error** at `xs[index] = value`, minimized to that assignment. The diagnostic includes an unstable FIR object identity, so the temporary probe was removed from the stable golden. Existing closed issue #252 already reports missing indexed-write support, so no duplicate bug was filed.
- Initial test startup under the environment's JBR 25.0.2 was a **harness failure** before test execution. Installed temporary Temurin 21.0.12.1, matching CI's JDK 21, and reran successfully.
- First full verification attempt was a **harness failure** because `z3` was absent. Installed temporary Z3 5.1.0 and reran the full pipeline.
- Read the complete `--update-goldens` report and both generated diagnostic files. The three recorded Viper warnings correspond exactly to the empty-list boundary, unsupported singleton-size fact, and mutable-list upper-bound controls; the absence of an array warning is the defect reported as #482.

## Commands

- `jbcontext search "Where are golden testData cases and converter implementations for Kotlin List, Array, primitive arrays, collection size, indexed get and set, including bounds verification?"`
- `./agent-scripts/test.sh collection_array_pairs`
- `./agent-scripts/test.sh --update-goldens collection_array_pairs`
- `./agent-scripts/test.sh --verify collection_array_pairs`
- GitHub searches across open and closed issues for indexed writes, native array size, array reads, and bounds obligations.
- `./agent-scripts/check-all.sh` under Z3 5.1.0: Gradle failed only at existing `testExists`; classified as solver-version mismatch.
- `./agent-scripts/check-all.sh` under the documented Z3 4.8.7: Gradle check and testData checks passed; exit 2 because `pre-commit` was unavailable.
- Attempted to install `pre-commit` in a temporary virtual environment; the package download was blocked by the environment proxy with HTTP 403.
- Ran the configured local hooks directly: `agent-scripts/check-testdata.sh` passed through `check-all.sh`, `agent-scripts/tests/run.sh` passed all assertions, and `git diff --check` plus explicit end-of-file inspection passed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=3f2cbe37-c3cd-4a24-bb84-de626512384f
