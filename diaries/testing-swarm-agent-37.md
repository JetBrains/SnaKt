# Testing swarm agent 37 diary

## Assignment

Executed [issue #354](https://github.com/JetBrains/SnaKt/issues/354), focused on `is`/`!is`, safe and explicit casts, nullable cast targets, branch-local type facts, and explicit casts that must fail at runtime.

## Actions

1. Read `AUTOMATIONS.md` before inspecting the repository and recorded the assignment in `automationsInstructions/testing-swarm-agent-37.md`.
2. Confirmed the checkout started on `implementing-air-automations`, then created `test/swarm-37-cast-type-refinement` from it. The branch name follows the existing `test/...` convention visible in pull requests targeting the automation branch.
3. Inspected the existing cast test data, generated Viper output, type-operator conversion in `StmtConversionVisitor`, and related verification tests before selecting probes.
4. Searched open and closed GitHub issues for cast, smart-cast, and type-test reports. No duplicate or already-reported defect matching these probes was found.
5. Added `cast_type_refinement.kt` with positive and negative controls for:
   - local facts in both `is` branches;
   - local facts in both `!is` branches;
   - safe casts of nullable values, including success and null-result branches;
   - a known-success explicit cast;
   - an explicit cast of null to a nullable target;
   - non-nullable and nullable-target explicit casts that must throw at runtime;
   - a deliberately invalid fact in the negative type-test branch.
6. The first fast-loop attempt failed before compilation because the environment selected Java 25.0.2. Installed Temurin Java 21 locally and reran.
7. The next conversion attempt exposed an unrelated test-shape limitation: direct `is` expressions passed to the vararg `verify` helper required the unsupported `java/io/Serializable` embedding. Minimized around it by storing each predicate in a local, and enabled the standard `FULL_JDK` fixture used by comparable verification tests.
8. Ran the fast conversion loop, then `--update-goldens`, and read the complete generated FIR output. It correctly encodes `is`/`!is` as subtype predicates, safe casts as a subtype-test/null choice, and explicit casts as target-type refinement on normal continuation.
9. Full verification initially could not start because Z3 was absent. Installed Z3 5.1.0 locally and reran.
10. Full verification showed the two necessarily failing explicit casts do **not** make their following `verify(false)` obligations pass. This conservatively avoids an unjustified proof assumption. The deliberately wrong branch fact also fails, while all positive controls verify.
11. Marked those three expected verification failures, regenerated the goldens, read the full update report, and confirmed the diagnostics are intentional.

## Conclusions

- `is` and `!is` conditions provide the correct fact in each local branch.
- A safe cast result is non-null exactly on the modeled target-subtype path; both the original value and result can be refined there, and the null-result branch proves the original is not the target type.
- Successful explicit casts and null-to-nullable-target casts preserve the expected facts.
- Explicit casts known at Kotlin runtime to fail do not let verification prove arbitrary claims afterward. They remain conservative false negatives because the model does not establish that a freshly constructed base-class object cannot also have the subtype; this is a precision limitation, not a confirmed soundness defect.
- No previously unreported bug was confirmed, so no `swarmTestingBug` issue was opened.

## Verification

- `./agent-scripts/test.sh cast_type_refinement`: conversion reached the expected missing-golden observation after the environment and fixture adjustments.
- `./agent-scripts/test.sh --update-goldens cast_type_refinement`: one golden rewritten; the report contained exactly the two expected `assert false` failures and the wrong-branch failure; test-data checks passed.
- `./agent-scripts/test.sh --verify cast_type_refinement`: 1 passed, 0 failed.
- Initial `./agent-scripts/check-all.sh`: Gradle failed in the unrelated existential test because Z3 5.1.0 had been selected, test-data checks passed, and pre-commit was skipped because it was unavailable.
- `./agent-scripts/test.sh --verify exists` with the documented Z3 4.8.7: 2 passed, 0 failed, confirming the earlier failure was solver-version drift.
- `./gradlew check --no-daemon` with Java 21 and Z3 4.8.7: passed.
- The Python package mirror blocked installation of the pre-commit runner. Ran every configured hook directly instead: the official `pre-commit-hooks` v5.0.0 end-of-file fixer passed without changes, `./agent-scripts/check-testdata.sh` passed, and `./agent-scripts/tests/run.sh` passed all assertions.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=525f3454-4f1b-462a-a86a-9ec215e36ef7
