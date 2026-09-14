# Testing swarm agent 36 diary

## 2026-09-14

- Read `AUTOMATIONS.md` before taking any repository action.
- Read the triggered issue assignment for issue #353 and its required testing protocol.
- Confirmed the checkout starts on `implementing-air-automations`, with no worktree changes.
- Inspected existing automation instruction and diary names, recent local history, and merged pull requests targeting the source branch to establish naming and delivery conventions.
- Recorded this assignment in `automationsInstructions/testing-swarm-agent-36.md` and created this diary.
- Created branch `test/floating-point-rejection` from `implementing-air-automations`, following the repository's `test/...` convention.
- Read `docs/agents-dev.md`, the statement conversion visitor, numeric embedding code, diagnostic definitions, and representative operator goldens.
- Confirmed that only integer, Boolean, character, string, and null literals are implemented; other literal kinds are intended to emit blocking conversion diagnostics in fail-closed mode.
- Added an initial focused probe matrix covering integer arithmetic as a positive control; Float and Double literals; arithmetic; ordering and equality; NaN; and positive and negative infinity.
- The initial fast-loop attempt could not start under the environment's JDK 25.0.2 (`What went wrong: 25.0.2`). Downloaded a temporary Temurin JDK 21 and reran with that supported runtime; no repository configuration was changed.
- Read the complete first-run diagnostics and generated FIR golden. The integer control converted to Viper. Float and Double literals, arithmetic, and comparisons each failed closed at the first unsupported literal with `INTERNAL_ERROR`; NaN and infinity failed closed at the unsupported class qualifier. No backend or verification stage was reached for unsupported functions.
- Updated source diagnostic markers to encode those observed failures. The diagnostic kind and message are stable and specific enough to reject unsupported floating-point constructs, though they are presented as internal errors rather than first-class unsupported-feature diagnostics.
- Reran the fast conversion loop successfully: 1 test passed.
- Ran `--update-goldens` and read its complete report plus the full generated FIR golden. It reported the new integer Viper output, all expected unsupported floating diagnostics, regenerated test registration, and a passing testData consistency check; the observations match the intended assertions.
- The first full verification attempt reached the backend but could not start because Z3 was absent. Installed the documented Z3 4.8.7 binary in a temporary directory and reran without repository configuration changes.
- Full focused verification then passed: 1 test passed. This confirms the positive integer control verifies and unsupported floating functions do not reach verification.
- Searched open and closed GitHub issues for Float, Double, NaN, and infinity. Existing issue #245 already records the missing floating-point model, and open issue #314 proposes sound floating-point semantics. No previously unreported defect was found, so no bug issue was opened.
- Ran `git diff --check`, inspected the generated test registration and worktree scope, and found no whitespace or unrelated changes.
- Ran `./agent-scripts/check-all.sh` with JDK 21 and Z3 4.8.7. Gradle check and testData checks passed, but the first run exited 2 because `pre-commit` was unavailable.
- Attempted the documented pip installation in an isolated virtual environment; the environment proxy rejected Python package downloads. Downloaded the official standalone `pre-commit` executable from its GitHub release instead and reran `check-all.sh`. Gradle and testData remained green, but hook environment creation hit the same proxy rejection while fetching setuptools.
- Executed every configured pre-commit check directly: the checked-out `end-of-file-fixer` normalized the new instruction file and passed on rerun; `agent-scripts/check-testdata.sh` passed; and all `agent-scripts/tests/run.sh` assertions passed. The only incomplete check is the pre-commit framework's environment bootstrap, due solely to the external package proxy.
- Prepared the focused golden tests, generated registration, instruction record, and this complete diary for commit and pull-request delivery to `implementing-air-automations`.
- Committed the test suite and records as `5a8afcf` (`Test floating-point rejection`) with the required Air Automations signature.
- Pushed branch `test/floating-point-rejection` and opened pull request #391 against `implementing-air-automations`.
