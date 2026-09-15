# Swarm matrix 34 diary

## Assignment

Issue [#464](https://github.com/JetBrains/SnaKt/issues/464) assigned compiled-Kotlin differential testing of control flow, calls, recursion, and evaluation order. I read `AUTOMATIONS.md` first and recorded the assignment in `automationsInstructions/swarm-matrix-34.md` before inspecting the implementation or tests.

## Environment and setup

- Started from `implementing-air-automations` at `9bac7b3` and created `test/swarm-464-control-flow-differential`, following the dominant `test/...` branch convention used by PRs targeting the automation branch.
- The initial fast test command could not start under the environment's Java 25.0.2 (`What went wrong: 25.0.2`). I installed Temurin 21.0.12.1 under `/tmp` and reran with that `JAVA_HOME`.
- Full verification initially reported a harness failure because `z3` was absent. I installed the repository-documented Z3 4.8.7 release under `/tmp` and set `Z3_EXE` for verifier runs.

## Inspection and probe choice

I inspected nearby control-flow, pure-function, recursion, negative-verification, and loop-invariant tests. Existing tests exercise the features separately, so the new focused matrix composes concrete Kotlin evaluation rules with verification claims:

1. A `when` subject with postfix increment checks that the subject is evaluated exactly once.
2. Two postfix-increment call arguments check Kotlin's left-to-right argument evaluation.
3. A false left operand followed by division by zero checks short-circuit evaluation and failure avoidance.
4. Two bounded recursive factorial calls nested inside another call check what callers can prove from a recursive function's deliberately weak contract.
5. An assertion that reverses the expected call-argument order is the negative control.

The probes are in `formver.compiler-plugin/testData/diagnostics/verification/control_flow/compiled_kotlin_differential.kt` with conversion and verification goldens.

## Compiled-Kotlin oracle

I compiled an annotation-free equivalent with the cached Kotlin 2.3.0 compiler and ran it on Temurin 21. The executable checked every expected value and printed:

```text
when=10 subject=2 ordered=12 next=3 shortCircuit=false recursive=62
```

Thus ordinary Kotlin evaluates the `when` subject once, evaluates the two arguments left-to-right, skips the unsafe division, computes the bounded nested recursive result as 62, and contradicts the negative control's proposed value 21.

## SnaKt results

Commands were run with JDK 21 and, for verification, Z3 4.8.7:

- `./agent-scripts/test.sh compiled_kotlin_differential`: passed after the new conversion golden was generated and inspected.
- `./agent-scripts/test.sh --verify compiled_kotlin_differential`: passed after the new verification golden was generated and inspected.
- `./agent-scripts/test.sh --update-goldens compiled_kotlin_differential`: passed, rewrote zero goldens, and reported the two intended warnings described below. `check-testdata.sh` also passed.
- `./agent-scripts/check-all.sh`: Gradle `check` and test-data checks passed. The wrapper exited 2 because `pre-commit` was unavailable. Installation in an isolated virtual environment was attempted with both pip and uv, but the environment's package-index proxy returned 403. I did not count this as a pass. I ran the configured local hooks directly: `agent-scripts/check-testdata.sh` passed, `agent-scripts/tests/run.sh` passed all assertions, `git diff --check` passed, and every added text/golden file has a final newline. The third-party `end-of-file-fixer` hook could not be installed, but its relevant file condition was checked directly.

Outcome classification:

- `whenSubjectEvaluatedOnce`: **supported and verified**. The Viper output saves the old subject value, increments once, branches on the saved value, and proves both assertions.
- `callArgumentsEvaluateLeftToRight`: **supported and verified**. The Viper output sequences the two increments before calling `decimalPair` with the saved values and proves 12 and final state 3.
- `shortCircuitSkipsUnsafeCall`: **supported and verified**. The division occurs only in the true branch of the generated conditional, and the false result verifies.
- `nestedRecursiveCallControl`: **expected proof failure**. Runtime returns 62, but the recursive function exposes only `result >= 1`; modular verification correctly cannot derive the exact factorial values at the caller.
- `negativeEvaluationOrderControl`: **expected proof failure**. Runtime returns 12 and the verifier correctly rejects the assertion that it is 21.
- Initial Java and missing-Z3 failures: **harness failures**, resolved by installing the documented prerequisites.

`--update-goldens` reported exactly the expected verifier warnings for `result == 62` and `result == 21`. I read the full conversion golden and confirmed that the evaluation order and branch structure match the compiled-Kotlin behavior.

## Existing issues and conclusion

I searched open and closed issues for evaluation order, short circuiting, postfix increments, calls, and recursion. Relevant assignments and known limitations include #355 (evaluation order), #350 (recursion), #372 (local-function conversion errors), #373 (mutual recursion), and closed #159 (recursion in contracts). No successful SnaKt verification contradicted compiled Kotlin, and the expected failures are sound and appropriately diagnosed. No new bug issue was filed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=b384877b-5d84-43e1-9b04-f3837df855aa
