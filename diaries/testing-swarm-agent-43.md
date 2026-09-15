# Testing swarm agent 43 diary

## 2026-09-15 — Issue #473

### Assignment and setup

- Read `AUTOMATIONS.md` before any other repository work and recorded the
  assignment in `automationsInstructions/testing-swarm-agent-43.md`.
- Created `test/issue-473-harness-contract-controls` from
  `implementing-air-automations`.
- Inspected the phased diagnostic harness, golden conventions, verifier facade,
  Silicon frontend, existing contract controls, and generated test registration.
- Used the Air-bundled Java 21 runtime because the default Java 25.0.2 runtime
  stops Gradle before tests with `25.0.2`. Downloaded the repository-documented
  Z3 4.8.7 binary into `/tmp` for verification runs.

### Probe

Added `harness_contract_mutation.kt` with two otherwise identical functions:

- Positive control `preservesExactResult` returns `value` under the postcondition
  `result == value`.
- Negative mutation `reversesExactResult` keeps the body but changes the
  postcondition to `result == value + 1`.

The conversion golden confirms that this single obligation is the material
difference in generated Viper. The positive control is supported and verified.
The negative control is an expected proof failure: Silicon reports only that
`reversesExactResult`'s postcondition might not hold.

### Commands and evidence

- `./agent-scripts/test.sh harness_contract_mutation` under Java 21 initially
  exited 1 while generating the missing conversion golden and source markers;
  after accepting those observations, it passed 1/1.
- `./agent-scripts/test.sh --verify harness_contract_mutation` without Z3
  exited 1 with `ExternalToolError: Cannot run prover ... 'z3': not a file`.
  Classification: backend failure, correctly propagated by the test runner.
- The same verification with `Z3_EXE` pointing to Z3 4.8.7 initially exited 1
  while generating the missing verification golden and diagnostic marker.
- `./agent-scripts/test.sh --update-goldens harness_contract_mutation` rewrote
  one golden and reported the complete conversion text, the one intended
  verification warning, the source diagnostic markers, and generated test
  registration. Every reported change matched the probe.
- Two consecutive `./agent-scripts/test.sh --verify
  harness_contract_mutation` runs then passed 1/1. Classification: deterministic
  supported/verified positive plus expected proof failure negative.
- `./agent-scripts/test.sh returns_booleans` passed 2/2, exercising existing
  positive and source-diagnostic contract controls in the conversion phase.
- `./agent-scripts/test.sh definitely_no_such_test` exited 1 and printed `No
  test matches`, confirming the documented harness-failure exit behavior.

The harness has no verifier timeout wrapper. Existing issue #283 already tracks
that gap, so no unbounded timeout probe was introduced. Existing issue #383
tracks Silicon backend abort handling, and issue #487 tracks wrapped golden
rewrite classification. Searches across open and closed issues for Z3, Silicon,
verifier timeout, golden harness, and postcondition behavior found no new defect
from this bounded probe, so no bug issue was filed.

### Final validation

- `./agent-scripts/check-all.sh`: Gradle `check` passed and testData checks
  passed. The wrapper exited 2 only because `pre-commit` was unavailable.
- Attempted the required installation in an isolated virtual environment; the
  package proxy rejected the pre-commit wheel with HTTP 403, so a clean wrapper
  exit 0 was not possible in this environment.
- Ran the locally configured checks directly: all agent-script assertions and
  testData checks passed; `git diff --check` passed; all added text files have a
  final newline. The full Gradle check already covered detekt, API checks, all
  module tests, verifier tests, locality tests, and plugin validation.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=6b15a8ad-e76c-4b0a-a51d-2224def474d9
