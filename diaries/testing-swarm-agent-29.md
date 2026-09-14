# Testing swarm agent 29 diary

## 2026-09-14 — Operator desugaring assignment

### Assignment and setup

- Read `AUTOMATIONS.md` before inspecting the repository.
- Recorded this run's assignment in `automationsInstructions/testing-swarm-agent-29.md`.
- Read issue [#346](https://github.com/JetBrains/SnaKt/issues/346), the test workflow documentation, existing operator verification tests, uniqueness operator tests, and the special-function conversion implementation.
- Created `test/issue-346-operator-desugaring` from `implementing-air-automations` at `9bac7b389dc5d2e01ffe14b0f92b69f134866c87`.
- Used the bundled OpenJDK 21.0.11 runtime because Gradle cannot start under the environment's default Java 25.0.2 runtime.

### Probes

- Added a focused golden test pairing custom operator syntax with explicit method syntax for `plus`, `minus`, `compareTo`, `get`, `contains`, `invoke`, `unaryPlus`, and `unaryMinus`.
- Covered both `<` and `>=` paths through `compareTo`, and both `in` and `!in` paths through `contains`.
- Added an explicit custom `set` call as the positive control for indexed assignment.
- Added a primitive `Int.plus`/`Int.minus` equivalence control.
- Read the complete generated conversion output rather than treating regeneration as success.

### Outcomes

- `plus`, `minus`, both `compareTo` comparisons, `get`, `contains`, `!contains`, `unaryPlus`, and `unaryMinus` resolve to the same Viper method calls as their explicit-method counterparts.
- The primitive control converts both syntaxes to identical native integer operations.
- Custom indexed assignment, `probe[index] = newValue`, fails conversion with an internal error for `FirUnitExpression`, while `probe.set(index, newValue)` converts to a call to the declared `set` method. The minimal failing probe was reproduced twice. Its diagnostic includes a nondeterministic FIR object identity, so it was not retained as a flaky golden. This is covered by the broader indexed-write report [#252](https://github.com/JetBrains/SnaKt/issues/252), which was closed after moving tracking upstream; no duplicate issue was opened.
- Custom invoke syntax diverges silently: `probe(argument)` is converted as an opaque function-object call and returns an unconstrained value, while `probe.invoke(argument)` calls the declared method. Searches of open and closed issues for invoke-operator, callable-object, and havoc terms found no report of this class-member resolution bug. Opened [#370](https://github.com/JetBrains/SnaKt/issues/370) with the minimal reproducer and label `swarmTestingBug`.

### Test evidence

- `./agent-scripts/test.sh desugaring`: passed after the stable golden was generated (`1` test, `1` passed).
- `./agent-scripts/test.sh --update-goldens desugaring`: conversion output and markers were inspected in full. Verification then failed for an environmental reason: Silicon could not run a prover because `z3` is not installed.
- `./agent-scripts/test.sh --verify desugaring`: reached the verification backend and failed before proof with `ExternalToolError: Cannot run prover at location 'z3': not a file`.
- `./agent-scripts/check-all.sh`: test-data validation passed; compilation, Detekt, plugin validation, and locality tasks completed, but the Gradle check failed because all 92 verification cases that reached Silicon encountered the same missing-Z3 startup error. The pre-commit check was skipped because `pre-commit` is not installed. The script therefore exited `1`, as required for the real Gradle-check failure.
- Tried to install both missing tools into an isolated Python virtual environment. The package proxy returned `403 Forbidden` for `files.pythonhosted.org`, so neither Z3 nor pre-commit could be installed in this environment.
- `./agent-scripts/check-testdata.sh`: passed after finalizing the stable golden.
- `git diff --check`: passed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=8502b7e2-de9f-4517-a914-c4ee4a522ef5
