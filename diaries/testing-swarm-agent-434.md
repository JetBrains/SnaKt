# Testing swarm agent 434 diary

## Assignment

Issue [#434](https://github.com/JetBrains/SnaKt/issues/434) assigned compiled-Kotlin differential testing of the Contract DSL and specification semantics. The assignment was recorded in `automationsInstructions/testing-swarm-agent-434.md` before investigation. Work was performed on `test/issue-434-contract-differential`, based directly on `implementing-air-automations` at `9bac7b3`.

## Exploration and probe selection

I read `AUTOMATIONS.md`, `AGENTS.md`, `docs/agents-dev.md`, `SPECIFICATIONS.md`, the Contract DSL runtime stubs, contract construction and propagation code, and nearby golden tests for implications, quantifiers, invalid specifications, and verification failures.

The key differential is that `Boolean.implies` is an ordinary infix function at runtime:

```kotlin
infix fun Boolean.implies(other: Boolean) = !this || other
```

Kotlin evaluates `other` before entering the function, but SnaKt lowers an invocation to Viper's logical implication. I selected a bounded division-by-zero consequent because it gives an objective runtime result and a directly comparable verifier obligation.

Before reporting, I searched all open and closed GitHub issues for contract, postcondition, precondition, quantifier, purity, implication, eager evaluation, short-circuit evaluation, and division-by-zero terms. Existing issues covered integer overflow (#296, #318), runtime differentials (#302, #362), quantifier purity (#288), specification well-formedness (#241), and other contract topics, but none reported eager evaluation of `implies`.

## Probes and results

Added `implies_runtime_differential.kt` with three probes:

1. `safeImplicationControl`: `verify(true implies (6 / 2 == 3))`. Classification: **supported and verified**. Compiled Kotlin completed normally.
2. `eagerImplicationRuntimeFailure`: `verify(false implies (1 / 0 == 0))`. Classification: **supported and verified by SnaKt, contradicted by compiled Kotlin**. SnaKt generated `assert false ==> 1 \\ 0 == 0` and verification succeeded. Kotlin 2.3.0 eagerly evaluated the consequent and threw `ArithmeticException: / by zero`.
3. `trueAntecedentDivisionBoundary`: `verify(true implies (1 / 0 == 0))`. Classification: **expected proof failure**. SnaKt reported `Assert might fail. Divisor 0 might be zero.` Compiled Kotlin also threw `ArithmeticException: / by zero`.

This confirms a soundness defect: logical implication lowering erases Kotlin's eager argument evaluation. I filed [#485](https://github.com/JetBrains/SnaKt/issues/485), applied `swarmTestingBug`, and did not apply `bugFound`.

## Commands and evidence

- Initial `./agent-scripts/test.sh implies_runtime_differential` failed before tests because Java 25.0.2 is unsupported. This was a **harness failure**, not a probe outcome.
- Installed Temurin 21.0.12.1 locally and reran the conversion loop: 1 test passed.
- Initial `--verify` could not find Z3. This was a **harness failure**, not a probe outcome.
- Installed the repository-required Z3 4.8.7 locally and reran verification: 1 test passed after adding the observed diagnostic markers.
- Compiled the equivalent unmarked source with Kotlin 2.3.0 plus `Annotations.kt` and `Builtins.kt`, then ran the jar. Output:

  ```text
  safeImplicationControl=completed
  eagerImplicationRuntimeFailure=ArithmeticException: / by zero
  trueAntecedentDivisionBoundary=ArithmeticException: / by zero
  ```

- Ran `./agent-scripts/test.sh --update-goldens implies_runtime_differential` with Java 21 and Z3 4.8.7. It rewrote 0 existing goldens, reported the new conversion output and the single intended Viper diagnostic, regenerated test registration, and passed `check-testdata.sh`. I read the entire report and the generated FIR golden. The recorded results match the classifications above.
- Ran `./agent-scripts/test.sh --verify implies_runtime_differential`: 1 test passed.
- Ran `./agent-scripts/check-all.sh`: Gradle `check` passed and `check-testdata.sh` passed, but the command exited 2 because `pre-commit` was unavailable. Installation in an isolated virtual environment and with `uv` was attempted; both were blocked by the environment proxy returning 403 for PyPI. I then ran the local pre-commit hook equivalents directly: `agent-scripts/tests/run.sh` passed all assertions, `agent-scripts/check-testdata.sh` passed, and `git diff --check` passed. The configured `end-of-file-fixer` could not run through pre-commit itself because the tool could not be installed.

## Conclusion

The safe control and true-antecedent boundary demonstrate that the runtime harness and verifier both detect the expected cases. The false-antecedent probe isolates the mismatch: SnaKt proves the mathematical implication without preserving evaluation of the consequent that compiled Kotlin performs. The focused golden retains this regression evidence, and #485 contains the minimal standalone report.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=b44c3ebb-f39c-4547-b471-17fca14458c4
