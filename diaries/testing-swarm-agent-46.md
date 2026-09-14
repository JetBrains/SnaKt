# Testing swarm agent 46 diary

## 2026-09-14 — Issue #363 diagnostic determinism

### Assignment

- Read `AUTOMATIONS.md` before taking any repository action.
- Recorded the run instructions in `automationsInstructions/testing-swarm-agent-46.md`.
- Worked from `implementing-air-automations` on the derived branch
  `test/issue-363-diagnostic-determinism`.
- Investigated [issue #363](https://github.com/JetBrains/SnaKt/issues/363): repeated runs and harmless source formatting changes must preserve diagnostic ordering, wording, and rendered source location.

### Investigation and test design

- Inspected `DiagnosticsCollector.render()`. It maps diagnostics to source ranges,
  severity, and rendered messages; sorts by first source offset and then message;
  renders the testData basename; separates diagnostics with one blank line; and
  normalizes trailing whitespace and the final newline.
- Inspected existing uniqueness-checker tests, their `.fir.diag.txt` goldens, the
  generated test runner, and the agent test documentation before choosing the
  probes.
- Added `diagnostic_determinism.kt` with:
  - a positive control showing that borrowing preserves a unique value without a diagnostic;
  - compact and expanded formatting variants that each produce two identical moved-access diagnostics in source order;
  - a boundary control that produces locality-mismatch and moved-access diagnostics on the same source range, exercising the lexical message tie-break.

### Runs and observations

- The environment initially selected Java 25.0.2. Three attempted focused runs
  exited 1 during Gradle configuration with `IllegalArgumentException: 25.0.2`;
  no tests ran, so these were classified as environment/backend setup failures,
  not test evidence.
- System package installation and direct JDK downloads were unavailable, but a
  bundled JBR 21.0.11 runtime was present in the Air agent distribution. All
  meaningful runs below used that runtime through `JAVA_HOME` and `PATH`.
- Ran `./agent-scripts/test.sh diagnostic_determinism` three times without update mode:
  - run 1 generated the initially absent observation golden and failed;
  - runs 2 and 3 passed and produced byte-identical logs.
- Read the complete generated `.fir.diag.txt`: four moved-access diagnostics
  appear first in source order, followed by the same-range boundary diagnostics
  in lexical message order (`Argument locality mismatch` before
  `Invalid access to moved reference`). Every rendered location is the stable
  `/diagnostic_determinism.kt` basename. The positive control emits no diagnostic.
- Ran `./agent-scripts/test.sh --update-goldens diagnostic_determinism` and read
  its complete report. It reported one test, zero rewritten goldens, displayed
  the six new diagnostics and diagnostic-marker source, regenerated the test
  registration, and passed testData validation. The observed output matches the
  intended assertions.
- Ran the focused conversion test three additional times without update mode.
  All three passed and their complete logs had the identical SHA-256 digest
  `009bd87d97eab6df392b6290db482c9180486aca0e30ff52ed651edc0416c9aa`.
- Ran `./agent-scripts/test.sh --verify diagnostic_determinism`: one test passed,
  zero failed.
- Ran `./agent-scripts/check-all.sh` under JBR 21. Gradle compilation, static
  analysis, and non-verification checks progressed, but the full Gradle check
  exited 1 because 91 pre-existing verification tests raised
  `viper.silicon.reporting.ExternalToolError`; this was classified as an
  unavailable external-backend failure. The testData check passed.
- `check-all.sh` reported pre-commit as skipped. A user-level installation was
  rejected by Python's externally managed environment, so a temporary virtual
  environment was created and installation retried there. The package proxy
  denied downloads with HTTP 403, leaving pre-commit unavailable; this check
  did not run.

### Conclusion

Diagnostic wording, ordering, and rendered source filename were deterministic
for repeated runs, harmless compact/expanded formatting, repeated equal-message
diagnostics, and two distinct diagnostics sharing a source range. Conversion
and full verification succeeded. No previously unreported bug was confirmed,
so no `swarmTestingBug` issue was opened.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=32c18ffe-51d7-41a2-bf31-eadce51fbabb
