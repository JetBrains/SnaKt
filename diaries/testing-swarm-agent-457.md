# Testing swarm agent 457 diary

## 2026-09-15 — Collections, arrays, and indexing state transitions

### Assignment and setup

- Read `AUTOMATIONS.md` before any repository action and recorded issue #457 in `automationsInstructions/testing-swarm-agent-457.md`.
- Started from `implementing-air-automations` at `9bac7b389dc5d2e01ffe14b0f92b69f134866c87` and created `test/issue-457-collection-state-transitions`, following recent swarm-test branch conventions.
- Used `jbcontext search` to locate the existing list, indexing, and bounds test data, then read the relevant tests and `StdLibConverter.kt`.
- The implementation models `List.get`, `List.subList`, `MutableList.add`, `emptyList`, and `isEmpty`. It has no indexed-write model. Existing tests covered isolated list operations but not short size-transition sequences or native-array controls.

### Probes

- `mutable_list_grows_step_by_step`: saved the initial size, added one element and safely read the newly valid index, then added a second element and read both the second new index and index zero.
- `sublist_size_transitions`: created an empty prefix and exercised its zero-length range, then created the whole-list range and reused the original size. This checks size facts after each transition and returns through the empty state.
- `empty_list_stays_empty`: exercised two equivalent zero-length ranges on an empty list.
- `nonempty_prefix_is_singleton`: guarded a source list, created a one-element prefix, read its only element, and exercised its full range.
- `one_add_does_not_make_second_index_safe`: negative boundary control. After one add, reading `initialSize + 1` must fail.
- `generic_array_read_at_zero` and `primitive_array_read_at_zero`: guarded read controls for `Array<Int>` and `IntArray`.
- `generic_array_unchecked_read` and `primitive_array_unchecked_read`: boundary controls with no non-empty precondition.
- Exploratory generic-array and primitive-array assignments each produced an `INTERNAL_ERROR` at `array[0] = 42`. This is unsupported conversion already reported by closed issue #252. The cases were minimized, then omitted from committed goldens because the diagnostic includes a nondeterministic FIR object identity hash.

### Commands and observations

- The initial `./agent-scripts/test.sh collection_state_transitions` could not start under Java 25.0.2. A bundled Java 21.0.11 runtime was selected, as required by the project build.
- Initial assertions written as `verify(list.size == ...)` produced the source diagnostic `Assert condition is impure`. Those invalid probes were replaced with index and sublist obligations that inspect the same modeled size transitions.
- Golden regeneration initially stopped because Z3 was absent. Downloaded the documented Z3 4.8.7 release with authenticated GitHub tooling into a temporary directory and set `Z3_EXE` for test runs.
- `./agent-scripts/test.sh --update-goldens collection_state_transitions` was read in full. It recorded one intended verification warning for the mutable-list upper-bound control. The list growth, empty, singleton, and range controls verified. Both unchecked array reads also verified with no diagnostic.
- The generated array `get` method requires only receiver and index types. Unlike `List.get`, it has no `0 <= index < size` precondition. Thus an empty array can reach a successfully verified read at index zero.
- `./agent-scripts/test.sh collection_state_transitions`: passed, 1 test.
- `./agent-scripts/test.sh --verify collection_state_transitions`: passed, 1 test. The sole recorded warning is the intentional mutable-list boundary failure.
- `./agent-scripts/check-all.sh`: Gradle `check` passed and test-data validation passed. The wrapper exited 2 only because `pre-commit` was unavailable. Installation was attempted with both pip and uv, but the environment proxy rejected `files.pythonhosted.org` with HTTP 403/tunnel errors.
- Ran the local hooks' substantive checks directly: `agent-scripts/tests/run.sh`, `agent-scripts/check-testdata.sh` (through `check-all.sh`), `git diff --check`, and final-newline checks all passed. The only unavailable check was the third-party `end-of-file-fixer` executable itself.

### Issue search and classification

- Searched open and closed issues for array assignment/internal errors, `IntArray.set`, array writes, array bounds, array index-out-of-bounds, and native-array `get` preconditions.
- Array writes: **internal error / known unsupported conversion**, duplicate of closed issue #252; no new report filed.
- List growth, sublist, empty, and singleton transitions: **supported and verified**.
- One-add second-index control: **expected proof failure**, correctly attributed as `POSSIBLE_INDEX_OUT_OF_BOUND`.
- Guarded array reads: **supported and verified**, though the generated model does not use the guard.
- Unchecked generic and primitive array reads: **incorrectly supported and verified**. This is a soundness defect distinct from issue #252 (writes) and issue #253 (size non-negativity).
- Filed [#490 — Array reads omit index bounds obligations](https://github.com/JetBrains/SnaKt/issues/490) with a minimal reproducer, expected and observed behavior, environment, control evidence, and the `swarmTestingBug` label.

Produced by Air Automations. Name: Testing swarm agent 457 / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=16b32161-89e3-43a2-8a31-8e0d631fccab
