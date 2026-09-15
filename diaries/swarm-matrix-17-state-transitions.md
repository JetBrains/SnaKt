# Swarm matrix 17: heap state transitions diary

## Assignment

- Trigger: [issue #447](https://github.com/JetBrains/SnaKt/issues/447)
- Area: heap objects, aliasing, and mutation
- Method: short state-transition sequences with observations after each step
- Run: [Testing swarm agent](https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=c9e695c9-5392-4f4f-9f39-9ea40990769a)

## Investigation

- Read `AUTOMATIONS.md` before other repository investigation and recorded the assignment in `automationsInstructions/swarm-matrix-17-state-transitions.md`.
- Confirmed the checkout started on `implementing-air-automations`, then created `test/issue-447-heap-state-transitions` using the repository's prevailing test-branch convention.
- Used semantic search to locate the closest class, constructor, property accessor, and unique-field golden tests.
- Inspected `classes/unique_fields.kt`, `classes/primary_constructors.kt`, `classes/property_accessors.kt`, and related verification controls before selecting probes.
- Compared open PR #396, which tests mutable-field permission transfer. Kept this suite distinct by testing ordered state observations through constructors, aliases, nullable references, nested replacement, mutation, and restoration.
- Searched open and closed GitHub issues for alias, mutation, constructor, nullable, and field-verification reports. The observed proof limitation is consistent with existing permission/specification work in #72, #211, and #276, so no new bug was filed.

## Probes and outcomes

- `mutateAndRestore`: construct `0`, mutate to `1`, restore to `0`, and inspect after each transition. Conversion succeeds. Field observations are expected proof failures because specification field reads are translated as unconstrained values.
- `observeMutationThroughAlias`: construct `10`, alias the object, mutate through the original to `20`, then through the alias to `30`. Conversion succeeds. The first field observation records the same expected proof failure; no mutation is incorrectly reported as verified.
- `nullableDetachAndReattach`: attach a fresh reference, detach to `null`, and reattach it. All three reference-state observations are supported and verified. This is the positive control. Kotlin also emits two expected source diagnostics noting that the fresh-reference null checks are always true.
- `replaceNestedCellAndMutate`: construct a uniquely owned nested cell, replace it, mutate the replacement, attach the original through a nullable field, and inspect every state. Conversion succeeds. Nested field observations record an expected proof failure at the first unsupported observation.
- `rejectStaleValueAfterMutation`: mutate `7` to `8`, then assert the stale value `7`. The verifier reports the expected proof failure. This is the negative control.
- No unsupported conversion, internal error, backend failure, timeout, or harness failure remained after installing the required local test dependencies.

## Commands and evidence

- `./agent-scripts/test.sh heap_state_transitions` initially exposed the environment's unsupported Java 25 default before tests ran.
- Installed Temurin Java 21 in `/tmp` and reran the focused conversion test. The first run generated the missing golden; the second passed.
- Installed the documented Z3 4.8.7 binary in `/tmp` after full verification reported that `z3` was absent.
- `./agent-scripts/test.sh --update-goldens heap_state_transitions` regenerated the observations. Read the complete 211-line FIR/Viper conversion golden and all four verification diagnostics. They match the intended classifications above.
- `./agent-scripts/test.sh --verify heap_state_transitions` passed: one test run, one passed, zero failed.
- `./agent-scripts/check-all.sh` completed Gradle and test-data checks successfully, but exited 2 because `pre-commit` was unavailable. Installation in an isolated virtual environment was attempted and blocked by the environment proxy returning HTTP 403 for PyPI.
- Ran both local pre-commit hooks directly: `./agent-scripts/check-testdata.sh` passed and `./agent-scripts/tests/run.sh` passed all assertions. `git diff --check` and final-newline checks also passed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=c9e695c9-5392-4f4f-9f39-9ea40990769a
