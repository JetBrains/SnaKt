# Swarm matrix 35 — model-based oracle testing

## Assignment and setup

- Read `AUTOMATIONS.md` first and recorded issue #465 in `automationsInstructions/swarm-matrix-35-model-oracle.md`.
- Confirmed the checkout started on `implementing-air-automations` at `9bac7b3` with no pre-existing worktree changes.
- Created branch `test/swarm-465-control-flow-oracle`, following recent `test/...` swarm branches.
- Read `docs/agents-dev.md` and nearby control-flow, call, recursion, pure-function, contract, and loop-invariant golden tests.
- Used `jbcontext search` to locate relevant coverage before selecting probes.
- The environment initially supplied JDK 25 and no Z3. The first Gradle run failed before tests with `25.0.2`; installed Temurin 21.0.12.1 and the repository-specified Z3 4.8.7 under `/tmp` and used them for subsequent commands. The missing/incompatible tools were harness failures, not SnaKt outcomes.

## Oracle and probes

Defined an independent bounded truth table for inputs `-2..2`:

| x | -2 | -1 | 0 | 1 | 2 |
|---:|---:|---:|---:|---:|---:|
| triangular(abs(x)) | 3 | 1 | 0 | 1 | 3 |
| score(x) | -3 | -1 | 7 | 11 | 23 |

Implemented the model with recursive pure calls, early returns, and `when`, plus an independent loop implementation with invariants. The positive control enumerates all five inputs against literal expected values and compares all five loop results with the recursive model. Calls to the loop implementation are assigned in source order before comparison. The negative control asserts the deliberately false boundary result `score(2) == 22`.

An initial early-return form exposed an unexpected precondition failure. Minimization produced `earlyReturnCallBoundary`: for `x` in `-1..0`, Kotlin returns before a later local initializer when `x < 0`, but generated Viper moves `recursiveTriangular(x)` before the condition. The resulting precondition failure is therefore a confirmed conversion-order defect rather than an expected proof failure. Rewriting the oracle as an `if` expression keeps the call branch-local and verifies.

## Commands and evidence

- `./agent-scripts/test.sh model_based_oracle` under Temurin 21: conversion passes after goldens were generated.
- `./agent-scripts/test.sh --update-goldens model_based_oracle`: read the full report. The final verification golden contains exactly two intended diagnostics: the confirmed unreachable-call precondition defect and the deliberately false negative control.
- `./agent-scripts/test.sh --verify model_based_oracle` with Z3 4.8.7: passes with the two expected diagnostics recorded.
- Searched open and closed GitHub issues for early returns, call preconditions, call hoisting, unreachable calls, and evaluation order. No existing report matched the defect.
- Filed #512, `Pure conversion hoists local initializer across early return`, with the minimized reproducer, expected/observed behavior, revision, environment, controls, and `swarmTestingBug` label.
- `./agent-scripts/check-all.sh`: final exit 0; Gradle checks, testData checks, end-of-file checks, and script tests all passed. The first run returned exit 2 because `pre-commit` was absent. Installed the official 4.6.2 zipapp; blocked PyPI access prevented its remote hook environment from fetching optional packaging dependencies, so the cached `pre-commit-hooks` environment was initialized offline for the configured end-of-file hook and the complete command was rerun successfully.

## Classification and conclusion

- Recursive calls, structured branches/`when`, bounded loop invariants, nested calls, and the complete five-value oracle: **supported and verified**.
- `score(2) == 22`: **expected proof failure**, correctly attributed to the assertion.
- Local initializer after an early return: **confirmed conversion defect**; a reachable-order violation creates an unreachable precondition obligation. Tracked as #512.
- Initial JDK 25 rejection and missing Z3: **harness failures**, resolved with the supported local tools.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=4e232c07-df5f-4fcd-af62-cbe071c04131
