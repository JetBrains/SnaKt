# Testing swarm agent 344 diary

## 2026-09-14

- Read `AUTOMATIONS.md` before beginning repository investigation.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-344.md` as required.
- Confirmed the checkout starts clean on `implementing-air-automations`.
- Created branch `swarm-testing/344-data-class-operations`, following the issue-focused swarm testing convention.
- Used semantic search and a read-only context explorer to identify the closest constructor-property and equality fixtures and trace generated data-class calls through ordinary function-call conversion.
- The first fast-loop run was blocked before tests by the environment's Java 25 runtime. Downloaded Temurin 17 to `/tmp/snakt-jdk17` and reran with that supported JDK.
- Added the constructor-property probe first. Its generated golden showed constructor field postconditions and property reads convert successfully; it also showed classpath diagnostics without `FULL_JDK`, so the fixture was corrected before further testing.
- Added separate focused probes for component functions, destructuring, copy, and structural equality, each with a positive and boundary/negative control.
- Installed the repository-required Z3 4.8.7 under `/tmp` after full verification initially reported the missing prover; reran all verification with `Z3_EXE` set.
- Confirmed constructor property reads preserve constructor values and verify successfully.
- Confirmed generated `component1`/`component2` calls convert, but their signatures contain no relationship to constructor properties; both focused postconditions fail verification as expected. Reported #424 after searching open and closed issues.
- Confirmed destructuring fails conversion on `FirComponentCall`. Reported #422 after a duplicate search. The generic error originally included a per-process FIR identity hash, so added a focused stable `visitComponentCall` diagnostic to make the golden reproducible without changing other unsupported-element diagnostics.
- Confirmed `copy(second = 30)` and `copy()` omit defaulted arguments in generated Viper and lack copy-result property contracts, yet the full pipeline accepts their field postconditions. Reported the unsound success as #425 after a duplicate search.
- Confirmed equal-valued distinct data-class instances lower to reference identity and fail the structural-equality postcondition, while a same-reference control verifies. Reported #423 after a duplicate search.
- Read the complete golden update reports. The recorded component proof failures, structural-equality proof failure, destructuring conversion failures, and successful controls match the observed behavior.
- Focused fast loop: `./agent-scripts/test.sh data_class_` — 5 passed.
- Focused full pipeline: `./agent-scripts/test.sh --verify data_class_` — 5 passed with Temurin 17 and Z3 4.8.7.
- Repository-wide Gradle check and testData check passed. `check-all.sh` could not complete its pre-commit stage because the environment lacked `pre-commit`; after installing the standalone runner, its isolated hook environment could not download `setuptools` because the configured Python package proxy returned HTTP 403.
- Ran the pre-commit checks available without that blocked package installation directly: `agent-scripts/check-testdata.sh`, `agent-scripts/tests/run.sh`, `git diff --check`, and an EOF check all passed.
