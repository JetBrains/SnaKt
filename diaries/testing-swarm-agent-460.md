# Testing swarm agent 460 diary

## Assignment

Issue #460 requests coverage-guided testing of collections, arrays, and indexing, including lists, mutable collections where supported, object and primitive arrays, size facts, get/set operations, empty and singleton cases, and bounds obligations.

## Actions

- Read `AUTOMATIONS.md` before taking any other repository action.
- Confirmed the checkout is on `implementing-air-automations` with no reported working-tree changes.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-460.md`.
- Inspected recent pull requests targeting `implementing-air-automations` and created `test/issue-460-collection-composition`, following the prevalent `test/issue-<number>-<topic>` convention.
- Inspected the List stdlib converter and nearby positive, negative, custom-list, and binary-search tests. Existing coverage models `List.get`, `MutableList.add`, `emptyList`, `isEmpty`, `subList`, and collection size; it has no native-array or indexed-write contracts.
- Added `feature_composition.kt` with six bounded probes built from minimal seeds: mutation then index zero, mutation then exact-size index, negative List index, guarded object-array read, singleton object-array write, and empty primitive-array read.
- The first conversion command, `./agent-scripts/test.sh feature_composition`, was a harness failure before tests ran because the environment supplied JDK 25.0.2, which Gradle's embedded Kotlin parser rejects.
- Installed Microsoft OpenJDK 21.0.12 into `/tmp` from the Microsoft Ubuntu package repository and reran the focused conversion test. This generated the initial conversion golden and exposed the expected `arrayOf(1)` vararg internal error.
- Ran `./agent-scripts/test.sh --update-goldens feature_composition` and read the complete report plus the generated conversion golden. The report showed the intended List contracts, unconstrained native-array calls, and the vararg internal error. `check-testdata.sh` passed.
- The first verification run was a harness failure because `z3` was absent. Downloaded Z3 4.15.8 from its official GitHub release into `/tmp` and placed it on `PATH` for subsequent test commands.
- Ran `./agent-scripts/test.sh --verify feature_composition`; the initial observation produced exactly two List diagnostics. Added `POSSIBLE_INDEX_OUT_OF_BOUND` markers to the exact-size and negative-index expressions.
- Ran `./agent-scripts/test.sh --update-goldens feature_composition` again and read all reported observations. It recorded two intended verification warnings: exact-size may exceed the List bound, and `-1` may be below zero. The regenerated test registration was retained as instructed, and `check-testdata.sh` passed.
- Ran `./agent-scripts/test.sh --verify feature_composition` after setting expectations: 1 test passed, 0 failed.
- Searched open and closed GitHub issues for array indexing/bounds, indexed writes, `IntArray`, `arrayOf`, and vararg conversion. Issues #246, #252, and #253 already document the observed vararg internal error, missing indexed-write model, and missing array size/bounds contracts. Issue #328 is the active swarm assignment for array indexing obligations. No new bug issue was filed.
- Ran `./agent-scripts/check-all.sh`. The testData check passed and the new stdlib List suite passed all 5 tests, including `feature_composition`. The overall Gradle check failed in the unrelated existing `Verification.User_invariants.testExists` golden comparison (143 tests, 1 failure); its XML identifies only `exists.kt` and `exists.viper.diag.txt`, neither touched by this branch. The pre-commit stage was skipped because `pre-commit` is absent; installation through the configured Python package path was blocked by the environment proxy. `git diff --check` passed.

## Outcome classification

- `add_then_first`: **supported and verified**. `MutableList.add` increases size, which proves index zero is valid afterward.
- `add_then_exact_size`: **expected proof failure**. An index equal to the current size violates the strict upper bound.
- `negative_list_index`: **expected proof failure**. The constant negative index violates the lower bound.
- `guarded_array_read`: **supported by conversion and verified, but without array bounds semantics**. `Array.isNotEmpty` and `Array.get` are unconstrained ordinary calls, so the guard does not establish a modeled bound.
- `singleton_array_write`: **internal error / known unsupported conversion**. The minimal `arrayOf(1)` seed reaches the existing vararg limitation before indexed assignment is converted; tracked by #246, with indexed writes tracked by #252.
- `empty_int_array_read`: **supported by conversion and verified, but without array bounds semantics**. The empty primitive-array read receives no bounds obligation; the missing native-array model is already documented by #253.

## Conclusion

The composition probes strengthen golden coverage for List lower and upper bounds and for the current native-array limitation. They confirm that mutation-plus-read reasoning works for Lists, while native object and primitive arrays remain outside the stdlib contract surface. All surprising array outcomes correspond to existing reports, so this assignment found no previously unreported defect.
