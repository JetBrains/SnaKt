# Swarm matrix 32 diary

Issue: https://github.com/JetBrains/SnaKt/issues/462

## Assignment

Test control flow, calls, and recursion with semantics-preserving metamorphic rewrites. Compare conversion, diagnostics, and verification, including positive and negative or boundary controls.

## Actions and evidence

- Read `AUTOMATIONS.md` before all other repository work.
- Recorded the assignment in `automationsInstructions/swarm-matrix-32.md`.
- Confirmed the starting branch was `implementing-air-automations` with a clean worktree before recording the assignment.
- Reviewed recent pull request branch names and created `test/issue-462-control-flow-metamorphic` from `implementing-air-automations`.
- Used semantic search to inspect existing control-flow, recursion, function-call, loop-invariant, and negative verification tests, then reviewed `docs/agents-dev.md` and nearby open testing pull requests to avoid duplicating Boolean-normalization and early-return coverage.
- Added `control_flow_metamorphic.kt` with paired recursive early-return/conditional-expression forms, equivalent loop guards and renamed loop binders, `if`/`when` desugaring, nested/sequential calls, and reversed independent call order. Added `dependentCallOrderBoundary` as a deliberately non-equivalent `@NeverVerify` control.
- Probed a local function with a renamed binder. Conversion produced `INTERNAL_ERROR: Not yet implemented` on the local declaration. Searched open and closed GitHub issues and found existing issue #372, “Local function declarations produce internal conversion errors,” with the same minimized reproducer and classification. Removed the duplicate local-function case from the committed golden and did not file another defect.
- The first fast-loop attempt was a harness/environment failure: Gradle 8.14.3 rejected the runner's JDK 25 before any test ran. Downloaded a temporary Temurin 21.0.8 runtime and reran without changing repository configuration.
- Generated and read the complete 233-line conversion golden. It preserves the same contracts across each pair while showing the expected structural differences: early-return versus expression branches, `>` versus negated equality loop guards, an extra subject temporary for `when`, nested-call temporary versus named local, and reversed independent call order.
- The focused conversion test passed after the golden was generated. The first full-pipeline golden update reached verification but failed because no Z3 executable was installed; classified this as a harness failure and did not record it as a test result.
- Downloaded the repository-specified Z3 4.8.7 to a temporary directory and confirmed its version. `./agent-scripts/test.sh --verify control_flow_metamorphic` then passed: all ten positive functions are supported and verified, and the one `@NeverVerify` boundary produces the expected proof failure.
- Ran `./agent-scripts/test.sh --update-goldens control_flow_metamorphic` with JDK 21 and Z3 4.8.7, read its report and the full conversion golden, and confirmed that it rewrote zero goldens. No verification diagnostic golden exists because the expected outcomes match the verification annotations. The testData consistency check and `git diff --check` passed.
- Ran `./agent-scripts/check-all.sh` with JDK 21 and Z3 4.8.7. The full Gradle check and testData check passed; the command exited 2 only because `pre-commit` was unavailable.
- Followed the exit-2 instruction and attempted to install `pre-commit` in an isolated temporary virtual environment. The package proxy rejected every download with HTTP 403. Ran the two local hooks directly: all agent-script tests and the testData check passed. Confirmed `git diff --check` passes and every changed file ends with a newline, covering the remaining end-of-file hook behavior.
- No new defect was found. Supported pairs converted and verified consistently; the negative control failed proof as expected; the only internal error was the already-reported local-function limitation in issue #372.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=1fa378dd-94e5-4438-9134-4e1ca1a937ef
