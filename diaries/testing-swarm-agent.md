# Testing swarm agent diary

## 2026-09-14 — Issue #331: default and named arguments

### Assignment and setup

- Read `AUTOMATIONS.md` before investigating the repository and recorded this run's assignment in `automationsInstructions/testing-swarm-agent.md`.
- Worked from revision `9bac7b389dc5d2e01ffe14b0f92b69f134866c87` of `implementing-air-automations` on branch `test/issue-331-default-named-arguments`.
- Read the test workflow in `docs/agents-dev.md`, inspected existing default-argument locality and uniqueness tests, and inspected function-call lowering in `StmtConversionVisitor` and `FirUtils`.
- Reviewed the earlier reports and fix in pull requests [#234](https://github.com/JetBrains/SnaKt/pull/234) and [#273](https://github.com/JetBrains/SnaKt/pull/273), plus issue [#236](https://github.com/JetBrains/SnaKt/issues/236), before deciding whether the observations were new.
- The environment initially provided Java 25.0.2 and no Z3. Installed temporary, workspace-external Temurin 21.0.12.1 and Z3 4.8.7 distributions to match project requirements; no repository configuration was changed for them.

### Probes added

- Added `default_named_arguments.kt` with paired omitted and explicitly expanded calls for:
  - a trailing default parameter;
  - a default parameter skipped in the middle with named arguments;
  - a default expression referring to an earlier parameter.
- Added a positional positive control and a reordered-named-argument comparison using the same values.
- Generated and inspected the full conversion and verification goldens and the generated test registration.

### Results and classification

- All Kotlin constructs were accepted; there was no unsupported-syntax or conversion failure.
- Explicitly expanded calls emitted one actual per formal. Their following `verify(false)` statements correctly produced proof failures.
- Each omitted call emitted fewer actuals than the callee has formals. The middle omission also shifted `third` into the `second` position. Silicon then aborted those methods with `NoSuchElementException: key not found: second`; this is a backend failure, and the test harness recorded no verification diagnostic for the aborted methods.
- The positional `subtract(3, 1)` control verified. `subtract(second = 1, first = 3)` was emitted as `subtract(1, 3)` and correctly failed the assertion expecting `2`, proving that source-order arguments were not mapped back to their named parameters.
- A default referring to an earlier parameter behaved like the other omissions: its expression was not supplied at the call site, and the generated call was undersaturated.
- These are not previously unreported bugs. They reproduce the argument-list root cause already documented in #234/#273 and the missing malformed-Viper/backend safety net tracked in #236. No `swarmTestingBug` issue was opened.

### Validation

- `./agent-scripts/test.sh default_named_arguments`: passed after the initial golden was generated and inspected.
- `./agent-scripts/test.sh --verify default_named_arguments`: passed with Java 21 and Z3 4.8.7 after the verification golden was generated and inspected.
- `./agent-scripts/test.sh --update-goldens default_named_arguments`: reported zero rewritten goldens; the complete report was read and its four proof diagnostics and malformed conversion output were confirmed as intended observations.
- `./agent-scripts/check-all.sh`: Gradle check and test-data validation passed. The command exited 2 because `pre-commit` was unavailable; an isolated installation attempt was blocked by the package proxy with HTTP 403, so that check could not run.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=dee33c71-c51e-4ee9-aa09-a4b54827aaa3
