# Testing swarm agent 41 diary

## Assignment

Investigate issue #358: whether deliberately failing verification cases emit
the intended diagnostics instead of being accepted merely because a failure is
recorded in a golden. Include positive and negative or near-boundary controls,
read all regenerated golden output, and report only new, minimized defects.

## Actions

- Read `AUTOMATIONS.md` before any other repository action.
- Recorded the assignment in
  `automationsInstructions/testing-swarm-agent-41.md`, as required.
- Confirmed the checkout started on `implementing-air-automations` and created
  `test/issue-358-golden-diagnostic-controls` from it.
- Read `AGENTS.md` and `docs/agents-dev.md` to understand the conversion-only,
  verification, golden-regeneration, and pre-delivery check workflows.
- Used semantic code search to locate the golden assertion implementation,
  golden reporting script, verification testData, and related documentation.
- Searched open and closed GitHub issues for prior reports. Issue #294 already
  tracks the general risk that a recorded proof failure makes a golden test
  pass; this is not a new defect to report.
- Inspected the existing `verify` intrinsic negative cases and their Viper
  diagnostics. They cover literal and compound failures plus a universally
  quantified success, but do not place positive, failing, and near-boundary
  controls together in a focused golden-integrity probe.
- Added `golden_diagnostic_integrity.kt` with three uses of `verify`: an
  unconstrained nonnegative assertion that must fail, the same assertion under
  its exact precondition as a positive control, and a strict assertion under a
  non-strict precondition that must fail at the zero boundary.
- The initial conversion run could not start under the environment's Java
  25.0.2 because Gradle 8.14.3 supports Java only through 24. Installed a
  temporary Temurin 17.0.20.1 JDK outside the checkout and used it for all
  subsequent test runs.
- Ran the focused conversion-only test. It reached the new test and generated
  the missing FIR golden; the Viper translation preserved all three intended
  assertions and translated the controls' `x >= 0` precondition to a method
  requirement.
- Ran `--update-goldens` once before Z3 was available. The complete report
  showed only the FIR output and reported a non-golden failure, so I did not
  treat that run as verification evidence.
- Ran focused verification and classified its failure as a backend environment
  failure: Silicon could not find the `z3` executable.
- Installed the repository-documented Z3 4.8.7 binary in a temporary directory
  outside the checkout and set `Z3_EXE` for subsequent runs.
- Reran `--update-goldens` and read its complete report. It recorded exactly
  two `Assert might fail` diagnostics: `x >= 0` without a premise and `x > 0`
  under the non-strict `x >= 0` premise. The exact-precondition positive control
  produced no verification diagnostic. The updater also inserted source
  diagnostic markers on exactly the two failing expressions. These results are
  semantically correct.
- Ran focused `--verify` against those recorded expectations: 1 test passed.
- Temporarily changed the first expected assertion text from `x >= 0` to the
  plausible but incorrect `x >= 1`, then reran focused `--verify`. The test
  failed and the recovered normalized diff identified that exact mismatch while
  preserving the second diagnostic. Restored the correct golden afterward.
- Reran focused `--verify` after restoration: 1 test passed.
- Ran `./agent-scripts/check-all.sh`. Gradle `check` passed and
  `check-testdata.sh` passed, but the command returned exit 2 because
  `pre-commit` was not installed.
- Attempted both a temporary virtual-environment install and `uv tool install`
  for pre-commit. The environment's proxy rejected downloads from
  `files.pythonhosted.org` with HTTP 403, so neither package installation could
  complete.
- Downloaded the official standalone pre-commit 4.6.2 zipapp from its GitHub
  release and reran `check-all.sh`. Gradle and testData checks passed again, but
  pre-commit's isolated hook installation hit the same proxy block while
  fetching setuptools, making a clean framework-level exit 0 unavailable in
  this environment.
- Executed every configured hook directly from the checked-out hook source:
  `end-of-file-fixer` over text files (excluding API dumps),
  `check-testdata.sh`, and `agent-scripts/tests/run.sh`. All passed; the script
  test runner reported all assertions passed.

## Conclusions

The focused cases distinguish verified behavior from proof failure: the exact
premise proves the assertion, while both the unconstrained and strict-boundary
assertions produce the intended Kotlin-associated `VIPER_VERIFICATION_ERROR`.
The wrong-golden probe confirms that an already recorded failure is accepted
only when the diagnostic content matches; a misleading diagnostic is rejected
with a readable expected/actual diff. The broader risk that any intentionally
recorded failure is thereafter a passing test remains the known design issue
#294. No previously unreported SnaKt bug was found, so no bug issue was opened.
