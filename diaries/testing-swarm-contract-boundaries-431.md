# Testing swarm: contract boundaries (#431)

## Assignment

Test Contract DSL and specification semantics using boundary-value analysis. Cover preconditions, postconditions, `verify`, quantifiers, implications, purity, contract propagation, accepted specifications, and invalid or impure forms. Add focused golden probes, classify results, search GitHub before filing defects, and deliver through a pull request based on `implementing-air-automations`.

## Actions and evidence

- Read `AUTOMATIONS.md` before any repository investigation.
- Recorded the assignment in `automationsInstructions/testing-swarm-contract-boundaries-431.md` as required.
- Confirmed the checkout began clean on `implementing-air-automations` at revision `9bac7b3`.
- Ran semantic search for existing golden coverage. Relevant suites include contract positive/negative cases, user preconditions and postconditions, implication specifications, pure functions, and `verify` with quantifiers.
- Reviewed recent pull requests and selected the established branch convention `test/issue-431-contract-boundaries`.
- Reviewed issue #361 and PR #405 to avoid duplicating the existing specification-mutation probes.
- Added `contract_semantics_boundaries.kt` with these partitions:
  - inclusive precondition/postcondition domain minimum, interior, and maximum: `-1`, `0`, and `1`;
  - immediately below the accepted domain: `-2`;
  - empty, singleton, and immediately-above-singleton quantified integer ranges;
  - a pure function in `verify` and an increment expression rejected as impure;
  - empty string, singleton string, and null inputs through a pure Kotlin contract whose conditional effect is propagated into Viper.
- The initial test setup failed because the environment defaulted to JDK 25.0.2. Located the bundled JDK 21.0.11 required by the project workflow and used it for all subsequent runs.
- The first golden update could not invoke `z3`. Installed the repository-documented Z3 4.8.7 into a temporary directory and reran with `Z3_EXE` set.
- Read the complete generated FIR and Viper diagnostic files after golden regeneration. The only verification diagnostics are the intended controls: the `-2` call cannot meet `value >= -1`, and the two-element quantified range disproves `i == 0` at `i = 1`.
- Confirmed the impure increment produces the source diagnostic `PURITY_VIOLATION` and skips verification, while all accepted/pure controls convert and verify.
- Ran `check-all.sh`: Gradle `check` and test-data validation passed. The wrapper returned exit 2 solely because `pre-commit` was unavailable.
- Tried both a user installation and an isolated virtual-environment installation of `pre-commit`; the first was blocked by the externally managed Python policy and the second by the environment proxy denying `files.pythonhosted.org`.
- Ran both local pre-commit hook commands directly (`agent-scripts/tests/run.sh` and `agent-scripts/check-testdata.sh`); all passed. Also checked end-of-file formatting directly, respecting the hook's documented `.api` exclusion, and ran `git diff --check`; both passed.
- Committed and pushed branch `test/issue-431-contract-boundaries`, opened PR #489 against `implementing-air-automations`, and applied the requested `swarmTestingDone` label to issue #431.

## Commands

```text
sed -n '1,240p' AUTOMATIONS.md
pwd
git status --short --branch
jbcontext search "Golden testData examples for contract DSL preconditions postconditions verify quantifiers implications purity and contract propagation"
gh pr list --state all --limit 30 --json number,title,headRefName,baseRefName,url
gh pr view 405 --json title,body,files,url
gh issue view 361 --json title,body,url,state,labels
git switch -c test/issue-431-contract-boundaries
./agent-scripts/test.sh contract_semantics_boundaries
./agent-scripts/test.sh --update-goldens contract_semantics_boundaries
./agent-scripts/test.sh --verify contract_semantics_boundaries
./agent-scripts/check-all.sh
python3 -m pip install --user pre-commit
python3 -m venv /tmp/snakt-precommit
/tmp/snakt-precommit/bin/pip install pre-commit
./agent-scripts/tests/run.sh
./agent-scripts/check-testdata.sh
git diff --check
```

## Conclusions

Supported and verified: inclusive preconditions/postconditions at their minimum, interior, and maximum; vacuous and singleton universal quantifiers; implications; pure specification calls; Kotlin conditional contract conversion and propagation for empty, singleton, and null values.

Expected proof failures: the immediately-below precondition input and the immediately-above-singleton quantified range.

Source diagnostic: mutation in a `verify` expression is rejected as impure with `PURITY_VIOLATION`, and verification is skipped for that declaration.

No unsupported conversion, internal error, backend failure, timeout, or harness failure remained after installing the required local JDK and Z3 dependencies. No product defect was found, so no bug issue was filed. All repository checks that could run passed; the `pre-commit` wrapper itself remained unavailable because its package download was proxy-blocked, while both project-local hooks and the relevant end-of-file check passed directly.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=0fab2e56-cacf-496b-b810-81d12519ffcf
