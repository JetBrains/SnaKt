# Testing swarm agent 440 diary

## Assignment

- Read `AUTOMATIONS.md` before taking any other repository action.
- Recorded issue #440 in `automationsInstructions/testing-swarm-agent-440.md`.
- Created `test/issue-440-contract-composition` from `implementing-air-automations`, following recent testing branch conventions.
- Investigated contract DSL and specification semantics with coverage-guided feature composition.

## Exploration

- Used semantic code search to locate contract construction and nearby tests for preconditions, postconditions, quantifiers, implications, verification, purity, and propagation.
- Read `ContractBuilder.kt`, `FormverFirBlock.kt`, the relevant `ProgramConverter.kt` contract path, `SPECIFICATIONS.md`, and existing `user_invariants`, `contracts`, `pure_functions`, and `purity` tests.
- Existing tests covered the individual features well, but did not compose a pure quantified contract with a verified caller, explicit `verify`, and a callee-precondition negative control.

## Probes and observations

Added `contract_feature_composition.kt` and its generated goldens.

1. `quantifiedSuccessor` combines `@Pure`, a precondition, a postcondition, universal quantification, and implication. Outcome: **supported and verified**.
2. `usesQuantifiedContract` calls that function under the required precondition, uses the propagated scalar postcondition in `verify`, and proves a caller postcondition. Outcome: **supported and verified**.
3. `violatesPropagatedPrecondition` changes only the argument to `-1`. Outcome: **expected proof failure**, attributed to the callee call and its `seed >= 0` precondition.
4. The initial composition also added a triggerless existential to `quantifiedSuccessor`. It caused the otherwise-positive function and each program importing it to fail. Bisecting by removing only that condition restored all positive controls. `triggerlessExistentialBoundary` retains the minimized form. Outcome: **expected proof failure caused by known solver/trigger incompleteness**, already reported in JetBrains/SnaKt #297 and tracked by #299; no duplicate bug was filed.
5. `impurePostcondition` changes a valid specification expression into a call to an unannotated function. Outcome: **internal error** rather than the expected source purity diagnostic: `PureLinearizer used to convert non-pure ExpEmbedding; operation freshAnonVar is not supported in a pure context.` Replacing the call with `value > 0` is the positive control and verifies.

## Issue search and reporting

- Searched open and closed JetBrains/SnaKt issues for existential witness failures, impure postconditions, specification purity violations, internal contract errors, and misplaced specification blocks.
- Confirmed the existential outcome is covered by #297/#299.
- Confirmed closed #241 concerns misplaced specification blocks and does not cover an impure call at the valid first-statement position.
- Found no report for the pure-linearization internal error. Opened JetBrains/SnaKt #483, `Impure function call in postcondition causes internal error`, with the minimal reproducer, expected/observed behavior, revision, Java/Z3 versions, control evidence, and the `swarmTestingBug` label.

## Commands and environment

- Initial `./agent-scripts/test.sh contract_feature_composition` could not configure Gradle under the environment's Java 25.0.2 runtime.
- Installed Temurin 21.0.12.1 under `/tmp` using the authenticated GitHub release path after direct downloads and system package installation were unavailable.
- Installed the repository-required Z3 4.8.7 under `/tmp`; confirmed `Z3 version 4.8.7 - 64 bit`.
- Ran `./agent-scripts/test.sh contract_feature_composition` during conversion development.
- Ran `./agent-scripts/test.sh --update-goldens contract_feature_composition`, read the complete generated FIR and verification diagnostics, and accepted only the minimized intended results above.
- Ran `./agent-scripts/test.sh --verify contract_feature_composition`: **1 test passed, 0 failed**.
- Ran `./agent-scripts/check-all.sh`: the full Gradle `check` and test-data checks passed. The wrapper returned exit 2 because `pre-commit` was unavailable.
- Tried both the environment-supported virtualenv installation and the user installation path for `pre-commit`. The user path was blocked by PEP 668, while the virtualenv package download was denied by the environment proxy with HTTP 403. The skipped check could not be installed in this environment. As direct fallback checks, `git diff --check` and `./agent-scripts/check-testdata.sh` passed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=09251386-986d-4f40-82d8-7bc670144cf5
