# Testing swarm agent diary

## Assignment

- Trigger: https://github.com/JetBrains/SnaKt/issues/432
- Area: Contract DSL and specification semantics
- Method: semantics-preserving metamorphic rewrites
- Required controls: at least one positive and one negative or boundary case

## Actions

- Read `AUTOMATIONS.md` before any repository investigation.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent.md`.
- Confirmed the checkout and `origin/implementing-air-automations` were both at `9bac7b389dc5d2e01ffe14b0f92b69f134866c87`.
- Created branch `test/contract-metamorphic-rewrites`, following the repository's recent `test/...` branch convention.
- Inspected existing contract, user-invariant, quantifier, implication, and purity testData and their goldens before selecting probes.
- Added `metamorphic_contracts.kt` with these bounded pairs:
  - split specification statements versus Boolean conjunctions in preconditions, postconditions, and `verify`;
  - renamed result and quantifier binders;
  - implication versus its equivalent disjunction;
  - propagated split and conjoined postconditions at call sites;
  - equivalent false implication/disjunction boundary controls;
  - postfix and prefix mutation as impure assertion controls.
- Regenerated and read the complete conversion and verification goldens. The split contracts lower to separate `requires`/`ensures`/`assert` clauses while conjunctions lower to conjunctions or short-circuit control flow; their proof behavior remains equivalent. Binder names normalize in Viper. The implication and disjunction positive controls both verify. Both deliberately false forms yield an expected assertion failure. Both mutation forms yield `PURITY_VIOLATION` source diagnostics and skip verification.
- Searched open and closed GitHub issues for contract/precondition/postcondition/implication/quantifier/purity and `forAll`/`verify`/implication combinations; no matching prior reports were returned. No defect was found, so no bug issue was filed.

## Commands and results

- `./agent-scripts/test.sh metamorphic_contracts` initially failed as a harness failure because the environment's JDK 25.0.2 was unsupported.
- Installed Temurin JDK 21.0.12.1 from the official GitHub release, matching the repository CI's JDK 21 requirement.
- The first `--verify` attempt failed as a harness failure because Z3 was absent.
- Installed Z3 4.8.7 from its official GitHub release, matching the repository CI pin.
- `./agent-scripts/test.sh --update-goldens metamorphic_contracts` recorded the observed conversion and the two intended proof-failure diagnostics; `check-testdata.sh` passed. Every reported golden change was inspected and accepted.
- `./agent-scripts/test.sh metamorphic_contracts`: 1 passed, 0 failed.
- `./agent-scripts/test.sh --verify metamorphic_contracts`: 1 passed, 0 failed.
- `./agent-scripts/check-all.sh`: Gradle check, testData checks, end-of-file fixer, and script tests all passed. The first run returned exit 2 because `pre-commit` was absent; after installing the official `pre-commit` zipapp and preparing its hook environment from available sources, the rerun returned exit 0.

## Outcome classification

- Split/conjoined contracts, renamed binders, quantified implication/disjunction, and contract propagation: **supported and verified**.
- False implication and equivalent false disjunction: **expected proof failure**.
- Prefix and postfix mutation in `verify`: **source diagnostic** (`PURITY_VIOLATION`).
- No unsupported conversion, internal error, backend failure, timeout, or product defect observed after harness dependencies were installed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=4fbc1d04-f48c-4401-a187-3b6ec52202b8
