# Testing swarm agent diary

## 2026-09-15 — Issue #436 contract feature interactions

### Assignment

- Read `AUTOMATIONS.md` before repository investigation and recorded the assignment in `automationsInstructions/testing-swarm-agent.md`.
- Worked from `implementing-air-automations` on `test/issue-436-contract-interactions`.
- Exercised pairwise interactions among preconditions, postconditions, `verify`, quantifiers, implications, purity, and specification propagation.

### Probes and classifications

- `successorWithImplications` combines a precondition with two implication postconditions: **supported and verified**.
- `useImplicationPostconditions` calls the preceding function and checks the propagated result with `verify`: **supported and verified**.
- `quantifiedIdentity` combines `@Pure`, a direct postcondition, and a triggered universal implication: **supported and verified**.
- `usePureQuantifiedPostcondition` combines a caller precondition, pure-call specification propagation, and two `verify` arguments: **supported and verified**.
- `falsePostconditionControl` returns its input while claiming a strictly larger result: **expected proof failure**, reported as a postcondition verification warning.
- `impureQuantifiedImplicationControl` indexes a `List` inside a universal implication: **source diagnostic**, reported as a purity violation with verification skipped.

The positive controls verified, the deliberately false specification failed at the expected postcondition, and the invalid impure form was rejected before verification. No internal error, backend failure, timeout, harness defect, or contradicted semantic expectation was found.

### Commands and evidence

- Used semantic code search to locate contract/specification tests, then read nearby precondition, postcondition, implication, quantifier, purity, and propagation cases.
- Generated the test runner with `./gradlew :formver.compiler-plugin:generateTests --no-daemon -q` under Temurin JDK 17.0.20.1.
- Ran `./agent-scripts/test.sh contract_feature_interactions`: one focused conversion test passed after reading the complete generated FIR golden.
- Ran `./agent-scripts/test.sh --verify contract_feature_interactions` with Z3 4.8.7: one focused full-pipeline test passed after reading the complete Viper diagnostic golden.
- The first full-pipeline attempt exposed a missing local `z3` executable; installing the repository-documented Z3 4.8.7 resolved that harness dependency.
- Searched open and closed GitHub issues for quantifier purity, precondition/postcondition propagation, contracts, verification, and implications. Results contained the current swarm assignment family and no existing report relevant to an unexpected outcome.
- Ran `./agent-scripts/check-all.sh`: Gradle checks and test-data checks passed. The wrapper returned exit 2 because `pre-commit` was unavailable; installation in an isolated virtual environment was attempted, but the package proxy rejected the download with HTTP 403.

### Conclusion

The tested pairwise combinations behave consistently with the DSL semantics and existing single-feature controls. No bug issue was filed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=19dd9a8e-1944-4d63-b53e-4595319e1aec
