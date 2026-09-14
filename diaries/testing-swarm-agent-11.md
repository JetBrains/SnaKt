# Testing swarm agent 11 diary

## 2026-09-14

- Read `AUTOMATIONS.md` before performing repository investigation.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-11.md` as required.
- Began this diary to document all actions and conclusions.
- Confirmed the checkout started on `implementing-air-automations` at `9bac7b3` and created branch `swarm-testing/328-array-indexing-obligations` from it.
- Inspected recent history and merged pull requests targeting the source branch; neither provided a branch-name example, so selected an issue-scoped descriptive branch name.
- Used semantic code search to locate existing indexed-access verification tests, then read the test driver documentation and the closest list bounds tests.
- Searched open and closed GitHub issues for prior array-indexing reports. Issues #252 and #253 already document that native arrays are outside the stdlib specification surface and that indexed writes were missing; both were closed after being moved to the contributor fork.
- Inspected current testData and converter references. Existing bounds tests cover lists, while native array bounds have no focused verification golden.
- Added separate focused read and write probes for `IntArray`, covering literal zero, computed zero, literal and computed last index, empty arrays, branch-refined valid access, and one-past-end controls.
- The initial fast-loop run could not start under the environment's JBR 25.0.2 (`Gradle` failed with `25.0.2`). Downloaded the official Temurin 21.0.12.1 JDK release and reran with that compatible runtime.
- The read probes converted successfully. Inspection of the generated Viper showed every `IntArray.get` has only receiver/index type preconditions: there is no non-negative index, upper-bound, or array-size invariant. Literal and algebraically equivalent computed indices differ only in their arithmetic expression and receive the same absent bounds obligations.
- The write probes consistently produced conversion `INTERNAL_ERROR` diagnostics (`FirUnitExpression ... Not yet implemented`) for literal zero, computed zero, last, computed last, empty, branch-refined, and one-past-end writes. This reproduces the already-reported issue #252, so no duplicate bug report was opened.
- Removed the write golden test after confirming its error details contain nondeterministic JVM object identity hashes. A regenerated golden changed every hash and therefore could not be a stable repository test; the write coverage and classification remain recorded here.
- Ran `--update-goldens array_index_reads` and read its complete report. It recorded the conversion golden and regenerated test registration; `check-testdata.sh` passed. The verification stage initially did not run because Z3 was missing.
- Read the repository Z3 setup instructions, downloaded the required official Z3 4.8.7 release, confirmed its version, restarted Gradle with `Z3_EXE`, and ran `--verify array_index_reads`: 1 test passed.
- Full verification accepts the empty-array and one-past-end reads because no bounds obligations are generated. This is a verified manifestation of the already documented native-array specification gap in issue #253, not a new bug; no duplicate issue was opened.
- Ran `check-all.sh`. Gradle checks and testData checks passed, but the command returned exit 2 because `pre-commit` was not installed.
- Attempted to install `pre-commit` in a temporary virtual environment; PyPI access was rejected by the environment proxy. Downloaded the official standalone pre-commit 4.6.2 release instead.
- The standalone runner could clone `pre-commit-hooks` v5.0.0, but hook environment installation also attempted a blocked PyPI download. Ran the exact `end-of-file-fixer` implementation from that tagged hook repository over repository text files, then ran `check-all.sh` with only that already-executed hook skipped. Gradle checks, testData checks, the local script tests, and the pre-commit stage all passed with exit 0.
- Restored `gradle/wrapper/gradle-wrapper.jar` after the manual hook invocation mistakenly inspected the binary as text; confirmed no change to it remained.
- Reran `--verify array_index_reads` after end-of-file normalization: 1 test passed.
