# Testing swarm agent — issue 442

## Assignment

Test heap objects, aliasing, and mutation with semantics-preserving metamorphic rewrites for [issue 442](https://github.com/JetBrains/SnaKt/issues/442). The assignment was recorded in `automationsInstructions/testing-swarm-agent-issue-442.md` before exploration. Work was performed on `test/issue-442-heap-alias-metamorphic`, based on `implementing-air-automations` at `9bac7b3`.

## Exploration

- Read `AUTOMATIONS.md`, `AGENTS.md`, and `docs/agents-dev.md`.
- Used `jbcontext search "golden testData cases exercising mutable class fields constructors getters setters aliases and heap mutation verification"` before selecting nearby tests.
- Inspected `classes/unique_fields.kt`, `manual_permissions.kt`, `property_accessors.kt`, `primary_constructors.kt`, `acc_precondition.kt`, `old.kt`, and `manualFolding.kt`.
- Inspected recent pull requests targeting `implementing-air-automations`; their branch names established the `test/issue-...` convention.
- Searched open and closed GitHub issues for `Serializable` embedding failures, mutable field verification, heap aliases, and mutation. Existing open issues [#376](https://github.com/JetBrains/SnaKt/issues/376) and [#377](https://github.com/JetBrains/SnaKt/issues/377), both labeled `swarmTestingBug`, cover the observed behavior. No duplicate issue was filed.

## Probes and observations

Added `heap_alias_metamorphic.kt` and its conversion and verification goldens.

- `scalarPositiveControl`: `1 + 1 == 2` is supported and verified.
- `scalarNegativeControl`: `1 + 1 == 3` is an expected proof failure, demonstrating that the backend rejects a false assertion.
- `directMutation` / `directMutationNegatedForm`: binder renaming and Boolean negation preserve the outcome. Both convert, omit the property write, read a fresh `havoc(intType())`, and produce the expected proof failure. This is the defect already tracked by #377.
- `aliasedMutationConversion`: assigning the unique receiver to a renamed local converts, but the subsequent setter is absent from Viper. Classification: supported conversion with unsoundly omitted mutation, already tracked by #377.
- `independentUpdatesLeftFirst` / `independentUpdatesRightFirst`: reordering independent setters produces equivalent Viper. Constructors and unique predicates are emitted, while both setters are absent. Classification: supported conversion with omitted mutations, already tracked by #377.
- `nullableMutationDirect` / `nullableMutationNegatedForm`: equivalent null tests convert to equivalent branches. In both, the setter is absent and the returned field read becomes `havoc(intType())`. Classification: supported conversion with omitted mutation/fresh read, already tracked by #377.

An early exploratory run without the standard `// FULL_JDK` directive failed conversion with `The embedding for type java/io/Serializable is not yet implemented`. Adding the directive used by neighboring tests restored normal conversion. This is consistent with #376 and was not retained as a probe or reported again.

## Commands and verification

- The first `./agent-scripts/test.sh heap_alias_metamorphic` used the environment's JDK 25 and failed before tests because Gradle does not support that runtime. Subsequent commands used the bundled JBR 21.0.11, matching CI's JDK 21.
- Installed Z3 4.8.7, the version pinned by `.github/workflows/gradle.yml`, under `/tmp` because no solver was installed and system package installation was unavailable.
- `./agent-scripts/test.sh --update-goldens heap_alias_metamorphic`: inspected the entire report. It recorded exactly three intended proof failures and Viper output showing omitted setters and havoc reads. `check-testdata.sh` passed.
- `./agent-scripts/test.sh heap_alias_metamorphic`: 1 passed, 0 failed.
- `./agent-scripts/test.sh --verify heap_alias_metamorphic`: 1 passed, 0 failed.
- `./agent-scripts/check-all.sh`: Gradle `check` passed and testData checks passed; exit 2 because `pre-commit` was unavailable. Installing it in a temporary virtual environment was blocked by the package proxy (HTTP 403). Ran the local hooks directly: `git diff --check`, `agent-scripts/check-testdata.sh`, and `agent-scripts/tests/run.sh` all passed. The remaining `end-of-file-fixer` hook has no substantive validation beyond file endings; all added text files end with newlines.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=041b39d3-b5b1-43a8-bf08-7a2dff0446c1
