# Testing swarm agent 475 diary

## Assignment

- Trigger: GitHub issue [#475](https://github.com/JetBrains/SnaKt/issues/475), labeled `swarmTesting`.
- Area: diagnostics, harness, and verifier backend.
- Method: bounded model-based oracle testing with positive and negative or boundary controls.
- Run: [Air Automations run](https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=27c55553-d1eb-465e-ac41-fbb888f034d4).

## Actions

- Read `AUTOMATIONS.md` before repository inspection or branch changes.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-475.md`.
- Confirmed the checkout started on `implementing-air-automations` at `9bac7b3` with only this run's new instruction file untracked.
- Confirmed GitHub authentication for `jetbrains-air[bot]`.
- Inspected recent pull requests targeting `implementing-air-automations` and created branch `test/issue-475-harness-oracle`, following the prevalent `test/...` convention.
- Read `docs/agents-dev.md` for the test driver, golden regeneration, diagnostic diff, and exit-code rules.
- Started a read-only context exploration of the diagnostics, golden harness, verifier backend, and nearby tests before choosing probes.

## Model and probes

The bounded oracle is the complete Boolean domain for material implication,
defined independently as `!antecedent || consequent`:

| Antecedent | Consequent | Oracle result | Expected classification |
|:-----------|:-----------|:--------------|:------------------------|
| false | false | true | supported and verified |
| false | true | true | supported and verified |
| true | false | false | expected proof failure |
| true | true | true | supported and verified |

- Added `verification/operators/boolean_implication_oracle.kt` with one
  `@AlwaysVerify` function per row. This keeps each solver result independently
  attributable and includes three positive controls and one negative control.
- The initial conversion-only run failed only because the new FIR golden and
  `VIPER_TEXT` markers did not yet exist. Its recovered diagnostics showed all
  four functions converted to Viper assertions.
- Installed Temurin 21.0.12.1 under `/tmp` after the host Java 25.0.2 runtime
  prevented Gradle from starting. The system package manager was unavailable,
  so the JDK was obtained from the official Adoptium GitHub release.
- The first full golden update without Z3 reached Silicon and failed with
  `ExternalToolError: Cannot run prover at location 'z3': not a file`. This is
  classified as a backend failure, not a proof or harness success.
- Installed the documented Z3 4.8.7 release under `/tmp`, set `Z3_EXE`, and
  repeated golden generation. The generated Viper programs matched the truth
  table, and the sole Viper diagnostic was attached to `trueImpliesFalse`:
  `Assert might fail. Assertion boolFromRef(anon) might not hold.` This is the
  expected proof failure; the other three rows emitted no verification error.
- Read the complete `--update-goldens` report, the FIR golden, Viper golden,
  inserted markers, generated test registration, and JUnit XML. Accepted the
  recorded semantic outcomes because all four match the independent oracle.
- Repeated the unchanged golden update: it reported zero rewrites. Then ran
  conversion once and full verification twice; all three runs passed with the
  same committed output and exit status 0.

## Failure-path and runner evidence

- Ran the existing `assert_statements` control. It passed with source-level
  `PURITY_VIOLATION` and `VERIFICATION_SKIPPED` diagnostics, classified as a
  source diagnostic that prevents verification.
- Ran the existing `factorial` full-verification control. It passed while
  preserving its expected Viper consistency diagnostic for a self-referential
  postcondition, classified as a consistency/conversion failure rather than a
  proof failure.
- Ran the existing `empty` conversion control. It passed while preserving the
  expected `INTERNAL_ERROR` golden, classified as an internal conversion error.
- Wrapped the focused full-verification command in a one-second external
  timeout. It exited 124, so timeout remains visible and is not reported as
  successful verification.
- Ran the driver with `definitely_no_such_test_475`. It printed `No test
  matches` and exited 1, confirming no-match harness failure behavior.

## Confirmed harness defect

- The first successful-backend golden update generated a FIR golden, a Viper
  golden, and diagnostic markers, but summarized the expected assertions as
  `0 golden(s) rewritten, 1 failed for other reasons`.
- The JUnit XML wraps the two expected `AssertionFailedError` instances in a
  `DefaultMultiCauseException`. `agent-scripts/junit_counts.py` considers only
  the outer failure type, so it increments `other_failed` instead of
  `assertion_failed`.
- Searched open and closed GitHub issues for update-golden rewrite reporting,
  multiple failures, and harness failures. No duplicate was found.
- Opened [#487 — Golden update misclassifies wrapped rewrite assertions](https://github.com/JetBrains/SnaKt/issues/487)
  with a minimal reproducer, expected and observed behavior, environment,
  controls, and the required `swarmTestingBug` label. Did not apply `bugFound`.

## Verification

- `./agent-scripts/check-all.sh`: Gradle `check` passed, including the full
  compiler/verifier test pipeline, Detekt, API checks, locality tests, and
  plugin validation; `check-testdata.sh` passed. The command exited 2 only
  because `pre-commit` was unavailable.
- Attempted to install `pre-commit` with both `pip` and `uv`; the environment's
  package proxy rejected `files.pythonhosted.org` with HTTP 403, so that
  prerequisite could not be installed.
- Ran the two substantive local pre-commit hooks directly:
  `agent-scripts/tests/run.sh` passed all assertions, and
  `agent-scripts/check-testdata.sh` passed.
- `git diff --check` passed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=27c55553-d1eb-465e-ac41-fbb888f034d4
