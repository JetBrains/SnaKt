# Testing swarm agent — matrix 48 diary

## 2026-09-15 — Issue #478

- Read `AUTOMATIONS.md` before other repository work and recorded the assignment in `automationsInstructions/testing-swarm-agent-matrix-48.md`.
- Confirmed the checkout started at `implementing-air-automations` revision `9bac7b3`, then created `test/issue-478-failure-propagation`, following the branch naming used by recent testing pull requests.
- Read the test-harness guidance in `docs/agents-dev.md` and inspected `agent-scripts/test.sh` plus its shared result-reporting helpers.
- Used semantic code search to locate the phased diagnostic runner, verification facade, Silicon adapter, and representative test data. Inspected prior testing PRs #403 and #410 to avoid duplicating their golden-integrity and timeout probes.
- Installed temporary test dependencies outside the checkout: the bundled JetBrains Runtime 21.0.11 and the repository-documented Z3 4.8.7 downloaded through GitHub CLI. The environment's Java 25 cannot run Gradle 8.14.3, and Z3 was initially absent.
- Ran `./agent-scripts/test.sh --update-goldens viper_verify` with `Z3_EXE=/definitely/missing/z3`. Before the fix it printed `1 failed for other reasons` for Silicon's `ExternalToolError` but returned exit 0. Classified this as a harness failure that can hide a backend failure.
- Searched open and closed GitHub issues and pull requests for update-golden failures, `failed for other reasons`, and harness exit status. Found no duplicate. Issue #383 concerns backend aborts hidden inside the Silicon adapter, while this failure reaches JUnit and is then suppressed by the shell runner.
- Added `update_task_failed_unexpectedly` and changed update mode to set and return `overall_status=1` when result parsing fails or JUnit contains a non-assertion failure. Assertion-only failures caused by golden rewriting remain expected.
- Updated `junit_counts.py` to recognize Gradle's `DefaultMultiCauseException` as a golden mismatch only when every listed direct cause is an assertion type. Added all-assertion and mixed-aggregate XML fixtures plus update-exit classification tests.
- Added `failure_stage_controls.kt`: an exact-precondition positive control that verifies, a strict-boundary assertion that produces the intended proof failure, and a Double literal that stops in conversion. Current SnaKt classifies the unsupported Double conversion as `INTERNAL_ERROR`; it never reaches verification.
- The first conversion-only run generated the FIR golden and showed the exact diagnostic markers. Added those markers and reran the fast loop successfully.
- Ran `--update-goldens failure_stage_controls` with Java 21 and Z3 4.8.7 and read the complete output. It recorded two generated Viper methods, one source-attributed `Assert might fail` verifier warning for `x > 0`, and one source-attributed conversion `INTERNAL_ERROR` for the unsupported Double constant. The exact-precondition control emitted no verifier warning. These observations are correct.
- Adjusted the aggregate classification after observing that a new test's simultaneous golden and tag rewrites are wrapped by Gradle in an assertion-only `DefaultMultiCauseException`. The focused parser fixtures now distinguish that expected aggregate from a mixed assertion/backend aggregate.
- Temporarily changed an existing expected verification diagnostic, then ran `--update-goldens viper_verify`. The runner rewrote one golden and returned 0; the update restored the original content, leaving no diff. This is the positive update-mode control.
- Repeated the missing-Z3 update probe after the fix. It printed Silicon's `ExternalToolError`, reported `1 failed for other reasons`, and returned 1. No golden content changed.
- Ran focused full verification of `failure_stage_controls` twice. Both runs passed, confirming stable conversion and verifier diagnostics.
- Ran the existing locality `assign_local` test through the harness. Three matching tests passed, and its FIR golden keeps the Kotlin `UNRESOLVED_REFERENCE` plus locality diagnostics attributed to `assign_local.kt`; classified as source diagnostics rather than verification results.
- Ran the known solver-heavy `z_function` verification under a 15-second outer bound. It produced no test result before the cap and returned 124. This remains the known external-timeout behavior covered by #283 and the backend result problem in #383; no unbounded regression case was added.
- Opened bug #522, `Golden update exits successfully after backend failure`, with a minimal reproducer, expected and observed exits, revision and environment details, both controls, duplicate-search evidence, the `swarmTestingBug` label, and the required automation signature.
- Ran `./agent-scripts/check-all.sh` with Java 21 and Z3 4.8.7. Gradle `check` passed in 4m 7s and `check-testdata.sh` passed; pre-commit was initially unavailable, yielding the documented exit 2.
- Attempted to install pre-commit with `uv`; the configured package proxy blocked the wheel download. Downloaded the official pre-commit 4.6.2 zipapp through GitHub CLI and reran `check-all.sh`. Gradle and testData checks passed again, but pre-commit's isolated hook setup could not download setuptools because the proxy returned HTTP 403.
- Ran every configured hook directly: end-of-file-fixer over all changed text files, `check-testdata.sh`, and `agent-scripts/tests/run.sh`. All passed. `git diff --check` also passed.

## Conclusions

- Supported and verified: the exact-precondition control verifies consistently.
- Expected proof failure: the strict-boundary assertion produces one correctly attributed verifier warning.
- Source diagnostic: the existing unresolved-reference control remains visible in the FIR diagnostic golden.
- Unsupported conversion/internal error: Double conversion fails closed before verification, currently reported as an internal error.
- Backend failure: a missing prover reaches JUnit as `ExternalToolError` and now makes golden-update mode return 1.
- Timeout: the solver-heavy control remains externally bounded and produces no falsely successful result within the cap.
- Harness failure: the prior exit-0 behavior was confirmed, fixed, regression-tested, and reported separately as #522.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=d1312072-f8a5-4c75-b406-c5854402ff20
