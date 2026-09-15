# Testing swarm agent 47 diary

## 2026-09-15 — Issue #477

### Assignment and setup

- Read `AUTOMATIONS.md` first and recorded the assignment in `automationsInstructions/testing-swarm-agent-47.md`.
- Confirmed the checkout started at `implementing-air-automations` revision `9bac7b3`, inspected recent pull-request branch names, and created `test/issue-477-harness-state-transitions` from that revision.
- Read `AGENTS.md`, `docs/agents-dev.md`, `agent-scripts/test.sh`, its result counter, the conversion and verification fixtures, and representative golden tests before selecting probes.
- Searched open and closed GitHub issues for harness, golden, verifier, backend, unsupported-conversion, and timeout reports. Relevant existing reports include #283 (no verification timeout), #294 (proof-success expectations), #358 (golden expectation integrity), #365 (unsupported syntax), and #383 (backend abort handling).
- The image initially provided Java 25.0.2, which Gradle rejected before running tests. Installed Temurin 17.0.20.1 under `/tmp` from the official Adoptium GitHub release. The first concurrent retry caused incremental-cache contention; stopped the affected Gradle processes, ran `./gradlew clean`, and kept all subsequent Gradle operations sequential.
- Full verification initially failed at the backend boundary because `z3` was absent. Installed the project-documented Z3 4.8.7 release under `/tmp`, set `Z3_EXE`, stopped stale Gradle daemons, and reran unchanged.

### Focused probe

Added `state_transition_sequence.kt` with three bounded sequences:

1. `transitionAndReturn`: `initial -> initial + 1 -> initial`, checking facts after both transitions. Outcome: supported and verified.
2. `zeroDeltaTransition`: `initial -> initial + 0`, a boundary control. Outcome: supported and verified.
3. `transitionPastReturnedState`: `initial -> initial + 1 -> initial -> initial - 1`, then asserts that the state is still `initial`. Outcome: expected proof failure, reported as `VIPER_VERIFICATION_ERROR` on the final assertion.

The first conversion without `FULL_JDK` produced an internal conversion error for `java/io/Serializable`; adding the standard test directive returned the probe to the intended type universe. This was test setup, not a product defect.

### State-transition observations

| Transition | Command | Exit | Observation and classification |
|---|---|---:|---|
| Missing expectations | `./agent-scripts/test.sh state_transition_sequence` | 1 | Missing conversion golden; harness failure as expected for new testData. |
| Generate expectations without solver | `./agent-scripts/test.sh --update-goldens state_transition_sequence` | 0 | Conversion golden generated, then backend failure (`Cannot run prover at location 'z3'`); update command preserved exit 0 by design and reported one non-golden failure. |
| Generate with Z3 | same update command with Java 17 and `Z3_EXE` | 0 | Generated conversion and verification goldens. Read the complete files: Viper statements preserve operation order; exactly the overshot-state assertion has a proof diagnostic. |
| Conversion after generation | `./agent-scripts/test.sh state_transition_sequence` | 0 | One test passed. |
| Full verification | `./agent-scripts/test.sh --verify state_transition_sequence` | 0 | One test passed; solver success and expected proof failure matched goldens. |
| Repeated full verification | same command | 0 | One test passed again with unchanged results. |
| Repeated regeneration | update command | 0 | Zero goldens rewritten; report repeated the untracked new golden contents for review. |
| No-match boundary | `./agent-scripts/test.sh definitely_no_such_state_transition_probe` | 1 | Printed the exact no-match diagnostic; expected harness failure. |
| Source diagnostic control | `./agent-scripts/test.sh assert_statements` | 0 | Existing purity/source diagnostics passed. |
| Internal diagnostic control | `./agent-scripts/test.sh user_invariants/empty.kt` | 0 | Existing expected `INTERNAL_ERROR` diagnostic passed. |
| External timeout | `timeout --signal=TERM 2s ... ./agent-scripts/test.sh --verify state_transition_sequence` | 124 | Bounded external timeout, with no misleading pass output. Issue #283 already tracks the lack of an intrinsic verifier timeout. |
| Recovery after timeout | stop Gradle, then full verification | 0 | Returned to the prior clean state; one test passed. |

No unsupported conversion, backend failure, or timeout was misclassified as a successful proof. No previously unreported defect was confirmed, so no `swarmTestingBug` issue was opened.

### Final checks

- `./agent-scripts/check-all.sh`: Gradle `check` passed and testData checks passed. The wrapper returned 2 solely because `pre-commit` was unavailable.
- Tried the prescribed `pip install pre-commit`; the system Python correctly required a virtual environment. Created a temporary virtual environment and retried, but the package proxy rejected `files.pythonhosted.org` with HTTP 403. The skipped pre-commit check therefore could not be installed in this environment.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=6fc9abcd-0a2f-4391-8171-0f0f7a0a8cf9
