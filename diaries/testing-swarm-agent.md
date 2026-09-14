# Testing swarm agent diary

## 2026-09-14

- Read `AUTOMATIONS.md` before taking any other repository action.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent.md`.
- Confirmed the checkout starts clean on `implementing-air-automations`.
- Reviewed the issue assignment and existing specification, branching, explicit-verification, and negative-diagnostic tests.
- Inspected recent pull-request branch names and created `test/issue-361-specification-mutation-negatives` from `implementing-air-automations`.
- Added four isolated positive/negative pairs covering a negated postcondition, shifted upper bound, removed string-index precondition, and swapped branch results.
- The initial fast-loop attempt could not start under the environment's default JDK 25.0.2. Located the bundled JDK 21.0.11 runtime and reran conversion successfully after generating and reviewing the new FIR golden.
- Full verification initially reported the missing `z3` executable. Downloaded the CI-pinned Z3 4.8.7 release locally and set `Z3_EXE` for verification.
- Read the complete golden-update report. The negative controls produced exactly four intended diagnostics: a negated postcondition failure, an assertion failure for `x < 3` at the admitted `x == 3` boundary, a `stringGet` precondition failure after removing non-emptiness, and a postcondition failure after swapping branch results. The four positive controls produced no verification diagnostics.
- Reran `./agent-scripts/test.sh --verify specification_mutation_negatives` with JDK 21 and Z3 4.8.7: 1 test passed, 0 failed.
- No product defect was found, so no separate bug issue was appropriate.
- Ran `./agent-scripts/check-all.sh`: Gradle checks and test-data checks passed, but the first run exited 2 because `pre-commit` was unavailable.
- Installed the official pre-commit 4.6.2 zipapp. Its third-party hook environment could not fetch `setuptools` because the automation proxy returned HTTP 403, so ran the checked-out `end-of-file-fixer` hook directly over all text files (including the new untracked files), then ran the complete wrapper with only that already-completed hook skipped inside pre-commit. The local `check-testdata` and `script-tests` hooks passed; the wrapper reported Gradle check passed, test-data passed, and pre-commit passed with exit 0.
