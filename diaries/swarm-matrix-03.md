# Swarm matrix 03 diary

## Assignment

GitHub issue [#433](https://github.com/JetBrains/SnaKt/issues/433) assigned contract DSL and specification semantics testing through contract mutation and negative controls. The assignment was recorded in `automationsInstructions/swarm-matrix-03.md` before repository exploration.

## Exploration

- Read `AUTOMATIONS.md`, `AGENTS.md`, and `docs/agents-dev.md` before selecting probes.
- Confirmed the checkout started on `implementing-air-automations` at `9bac7b3`, then created `test/issue-433-contract-mutation` using the repository's current testing branch convention.
- Inspected existing contract and specification coverage, especially:
  - `user_invariants/simple_postcondition.kt` for precondition/postcondition propagation through calls.
  - `user_invariants/forall_with_triggers.kt` and `user_invariants/exists.kt` for quantified specifications and solver boundaries.
  - `user_invariants/factorial.kt` for implications in recursive specifications.
  - `contracts/positive/returns_booleans.kt` and `contracts/negative/returns_booleans.kt` for Kotlin contract DSL mutation pairs.
  - `contracts/negative/viper_verify.kt` for expected verification failures.
  - `purity/assert_statements.kt` and `purity/wrongly_annotated.kt` for impure specification expressions and invalid `@Pure` functions.
  - `old.kt` for stateful postcondition propagation.

## Probe design and observations

Added `user_invariants/contract_mutation_controls.kt` with two valid producer contracts and two callers. Both producers return `base + 1`. Their postconditions differ by one character:

```text
(base >= 0) implies (result > base)
(base >= 0) implies (result >= base)
```

Both callers assume `base >= 0`, call the corresponding producer, and verify `result > base`.

- **Positive control — supported and verified:** the strict producer postcondition propagates to the caller and proves the strict assertion.
- **Negative control — expected proof failure:** weakening `>` to `>=` preserves successful conversion and producer verification but no longer proves the caller's strict assertion. Silicon reports only `Assert might fail` for that assertion, recorded with `VIPER_VERIFICATION_ERROR`.
- The generated Viper confirms that the caller uses the callee specification rather than its implementation: each caller contains an abstract method declaration with the corresponding `ensures` clause.
- An initial exploratory version put the implication under a triggerless universal quantifier. Silicon could not use that quantifier to prove the intended positive control and warned that no trigger was inferred. Because that failed to isolate the mutation and duplicated an already documented solver boundary, the probe was minimized to the direct implication above. This was a test-design limitation, not a product defect.
- Existing quantifier and purity cases behaved as the nearby source/golden evidence describes; this assignment found no contradiction in supported semantics or failure handling. No new bug issue was filed.

## Commands and outcomes

- `./agent-scripts/test.sh contract_mutation_controls` initially failed before tests because the environment supplied JDK 25.0.2. Classified as **harness failure**.
- Installed a temporary Temurin JDK 21 matching CI and reran. Conversion generated the new FIR golden successfully.
- `./agent-scripts/test.sh --verify contract_mutation_controls` then reported missing `z3`. Classified as **harness failure**.
- Installed the repository-documented Z3 4.8.7 temporarily and set `Z3_EXE`.
- `./agent-scripts/test.sh --update-goldens contract_mutation_controls` reported one intended Viper diagnostic for the negative assertion and the expected strict/non-strict Viper postconditions. Read the complete generated FIR and Viper goldens and confirmed both are correct.
- `./agent-scripts/test.sh contract_mutation_controls`: **1 passed, 0 failed**.
- `./agent-scripts/test.sh --verify contract_mutation_controls`: **1 passed, 0 failed**.
- `./agent-scripts/check-all.sh`: Gradle `check` and testData checks passed; the command returned exit 2 only because `pre-commit` was unavailable.
- PyPI access was blocked by the environment, so `pre-commit` could not be installed. Ran the configured checks directly instead: `agent-scripts/tests/run.sh` passed all assertions, `agent-scripts/check-testdata.sh` passed as part of `check-all.sh`, and the exact `end-of-file-fixer` v5.0.0 hook passed on every changed file.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=2af9e9e9-fe87-483a-99f1-ecd42660e504
