# Testing swarm agent 33 diary

## 2026-09-14 — Issue #350 recursion handling

### Assignment

- Read `AUTOMATIONS.md` before all other repository investigation and recorded the assignment in `automationsInstructions/testing-swarm-agent-33.md`.
- Created `test/issue-350-recursion-handling` from `implementing-air-automations` at `9bac7b389dc5d2e01ffe14b0f92b69f134866c87`.
- Inspected the existing direct recursion, factorial, recursive sum, conditional contract, and pure-function tests before adding probes.

### Probes and observations

- Added `recursion_variants.kt` with direct and mutual recursion, decreasing non-negative integer arguments, stationary recursion, and direct and mutual `@Pure` recursion.
- Confirmed conversion emits recursive Viper methods for non-pure functions and recursive Viper functions for `@Pure` functions.
- Confirmed a decreasing recursive method with a correct `result == 0` postcondition verifies.
- Confirmed the negative control, a decreasing recursive method with the false `result == n` postcondition, receives `VIPER_VERIFICATION_ERROR`.
- Confirmed mutually recursive methods with decreasing integer arguments verify.
- Confirmed stationary method recursion verifies quickly. This is expected under partial-correctness semantics: the contract is established for any return, while termination is not claimed.
- Confirmed a direct decreasing recursive `@Pure` function verifies quickly, with no unsupported-recursion or consistency diagnostic.
- Confirmed mutually recursive decreasing `@Pure` Boolean functions convert but fail verification. Silicon reports the generated Boolean return-type postcondition twice for the first function rather than reporting unsupported recursion or a well-foundedness failure. The run completed quickly, so this is neither solver divergence nor unsound success.
- Read the complete golden-update report and retained the verification diagnostics as observations.

### Environment and defect report

- The environment initially supplied Java 25.0.2, which Gradle rejected before tests ran. Downloaded Temurin 21.0.12.1 to a temporary directory and reran with that supported runtime.
- Full verification initially stopped because Z3 was absent. Downloaded the repository-documented Z3 4.8.7 release to a temporary directory and set `Z3_EXE`; verification then ran normally.
- Searched open and closed SnaKt issues for recursion, mutual recursion, and pure recursion. Issue #159 concerned a resolved stack overflow in recursive contracts; no issue covered this mutual-recursion result. Also reviewed open PR #282, which addresses pure-function termination measures but does not provide this misleading diagnostic on the tested base revision.
- Filed [#373 — Mutually recursive @Pure functions produce misleading return-type failures](https://github.com/JetBrains/SnaKt/issues/373) with the minimal reproducer, expected alternatives, exact observed diagnostic, environment, revision, assignment reference, and `swarmTestingBug` label.

### Verification

- `./agent-scripts/test.sh recursion_variants`: passed after the initial golden was generated and inspected.
- `./agent-scripts/test.sh --verify recursion_variants`: passed with the intentional negative and defect diagnostics recorded.
- `./agent-scripts/test.sh --update-goldens recursion_variants`: passed; one verification golden was rewritten, the complete report was reviewed, and test-data checks passed.
- `./agent-scripts/check-all.sh`: Gradle check and test-data validation passed. The wrapper returned exit 2 solely because `pre-commit` was unavailable. Installation was attempted first with the system Python (blocked by its externally managed policy) and then in a temporary virtual environment (blocked by the package proxy with HTTP 403).
- Ran the available pre-commit checks directly: `agent-scripts/tests/run.sh`, `agent-scripts/check-testdata.sh`, `git diff --check`, and final-newline checks all passed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=9e58815c-44c5-42a6-891c-2b3eeed1ca41
