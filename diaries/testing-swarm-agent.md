# Testing swarm agent diary

## Assignment

Issue [#474](https://github.com/JetBrains/SnaKt/issues/474): test diagnostics, harness, and verifier-backend behavior with bounded compiled-Kotlin differential probes. Include positive and negative or boundary controls, inspect golden updates as observations, classify outcomes, search existing issues before filing any defect, and run proportional checks.

## Activity

- Read `AUTOMATIONS.md` before taking any other repository action.
- Confirmed the checkout started on `implementing-air-automations` with a clean worktree.
- Reviewed recent pull requests targeting the source branch and selected the established `test/issue-<number>-<topic>` branch convention.
- Searched nearby operator, contract, diagnostic, harness, and verifier tests. Existing issue #296 and issue/PR #318 cover integral overflow; issue #382 covers negative remainder semantics; issue #383 already covers a Silicon abort being mistaken for success. I therefore avoided duplicating those findings.
- Added `division_runtime_differential.kt` with two bounded cases:
  - `positiveDivisionControl`: `84 / 2`, followed by `verify(result == 42)`. Classification: supported and verified.
  - `divisionByZeroBoundary`: divides one by a local zero. Classification: expected proof failure at `divInts`'s nonzero precondition.
- The initial conversion run could not execute under the inherited JDK 25.0.2. `test.sh` reported no test results, correctly distinguishing an environment/harness setup failure from a pass. Installed a session-local Temurin 21.0.12.1 runtime after unprivileged apt installation was unavailable.
- The first golden update reached conversion but failed before verification because Z3 was absent. Its JUnit result reported `ExternalToolError: Cannot run prover at location 'z3': not a file`; this is a backend setup failure, not a solver outcome. Installed session-local Z3 4.8.7 as required by the README.
- Ran `./agent-scripts/test.sh --update-goldens division_runtime_differential` with JDK 21 and Z3 4.8.7. Read the complete report: conversion produced the expected two Viper methods; verification produced exactly one diagnostic on the zero divisor; source markers placed `VIPER_TEXT` on both functions and `VIPER_VERIFICATION_ERROR` only on `1 / zero`; test-data checks passed. Accepted these observations as the intended goldens.
- Compiled an ordinary standalone Kotlin version with Kotlin 2.0.21 and ran it on JDK 21. It printed `positive=42` and `boundary=ArithmeticException: / by zero`. This agrees with SnaKt: the positive control verifies and the runtime-failing boundary does not.
- Ran the focused conversion loop once and the full verification loop twice. All three runs reported `Ran 1 tests, 1 passed, 0 failed`, confirming stable golden replay and repeated verifier behavior.
- Exercised representative classifications and runner exits:
  - `./agent-scripts/test.sh assert_statements` replayed expected source/purity diagnostics and exited 0.
  - `./agent-scripts/test.sh exists_list_get_crash` replayed an unsupported impure quantifier-body conversion plus verification skip and exited 0.
  - `./agent-scripts/test.sh definitely_no_such_test` printed `No test matches` and exited 1.
  - Bounded `./agent-scripts/test.sh --verify z_function` externally at 30 seconds; it did not complete and `timeout` exited 124. No golden was updated, so the timeout was observed without recording it as success or proof failure. Issues #283 and #383 already cover verifier timeout/backend-result handling.
- Searched open and closed GitHub issues for division-by-zero, runtime exceptions, overflow, backend failure, and related differential testing. The new probe found no previously unreported defect, so no bug issue was opened.
- Ran `./agent-scripts/check-all.sh`. Gradle `check` passed in 4m17s and `check-testdata.sh` passed. The wrapper exited 2 solely because `pre-commit` was unavailable. A session-local installation attempt was blocked by the environment proxy with HTTP 403, so I ran every configured hook directly: `agent-scripts/tests/run.sh`, `agent-scripts/check-testdata.sh`, the end-of-file check over all changed text files, and `git diff --check`; all passed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=b264a602-f52d-46b8-9bc5-d71b8edfa44a
