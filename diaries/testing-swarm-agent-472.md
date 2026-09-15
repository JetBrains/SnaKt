# Testing swarm agent — issue 472 diary

## Assignment

- Area: diagnostics, harness, and verifier backend.
- Method: semantics-preserving metamorphic rewrites.
- Required controls: at least one positive and one negative or boundary case.
- Trigger: https://github.com/JetBrains/SnaKt/issues/472

## Actions

- Read `AUTOMATIONS.md` before investigating the repository.
- Recorded the issue instructions in `automationsInstructions/testing-swarm-agent-472.md`.
- Confirmed the checkout was on a clean `implementing-air-automations` branch tracking the required source branch.
- Inspected existing automation instruction and diary filenames, and the available local and remote branches.
- Used semantic code search to locate verification golden conventions and related Boolean, quantifier, and control-flow coverage.
- Read `docs/agents-dev.md`, the focused test driver, the full-check driver, and nearby positive and negative verification fixtures.
- Confirmed GitHub CLI authentication and inspected recent pull requests targeting `implementing-air-automations`; created `test/issue-472-metamorphic-diagnostics` from the required source branch, following the dominant `test/issue-<number>-<topic>` convention.
- Reviewed related open pull requests for Boolean normalization (#417), diagnostic determinism (#380), and independent statement reordering (#399) to avoid duplicating their probes.
- Searched open and closed GitHub issues for metamorphic diagnostic, binder-renaming, and diagnostic-determinism reports; found only the current assignment and a separate repeatability assignment.
- Added a bounded alpha-renaming testData case. Two positive controls differ only in postcondition and nested quantifier binder names; two negative controls differ only in the impossible postcondition binder name.
- The first focused conversion command could not start under the environment's default Java 25.0.2 (`Build failed: 25.0.2`). Located the bundled JetBrains Runtime 21.0.11 and used it for all subsequent Gradle commands.
- Ran `./agent-scripts/test.sh binder_alpha_renaming` with Java 21. The expected first run generated the missing conversion golden and exited 1. Read the complete generated file: each renamed pair lowered to identical specifications and bodies apart from method names.
- Ran `./agent-scripts/test.sh --update-goldens binder_alpha_renaming`. Its first attempt classified as a backend failure because Z3 was absent: `ExternalToolError: Cannot run prover at location 'z3': not a file`.
- Installed the repository-documented Z3 4.8.7 release in a temporary directory and confirmed its version.
- Re-ran golden update with Java 21 and Z3 4.8.7. It exited 0, rewrote one verification golden, and reported exactly two intended warnings: the alpha-renamed negative controls both failed the same impossible postcondition. The two positive controls emitted no verifier warnings. Read and accepted the full update report; `check-testdata.sh` passed.
- Ran `./agent-scripts/test.sh --verify binder_alpha_renaming` twice. Both runs passed (one test, zero failures), and SHA-256 hashes of both goldens were unchanged across the repeated run.
- Exercised an existing source-diagnostic control with `./agent-scripts/test.sh invalid`: one test passed, confirming expected frontend diagnostic handling.
- Exercised an existing conversion-bailout control with `./agent-scripts/test.sh assert_statements` and `--verify assert_statements`: both passed. Its purity violations are source diagnostics and the affected functions are classified as `VERIFICATION_SKIPPED`, rather than an internal or backend failure.
- Exercised test-runner no-match behavior with `./agent-scripts/test.sh definitely_no_such_fixture_472`: it printed the exact no-match message and exited 1 as designed.
- Exercised the documented unstable expensive verifier case with a 120-second external bound: `./agent-scripts/test.sh --verify z_function` completed normally in this run (one test passed, zero failures), so this run's solver outcome is supported and verified; neither a verifier timeout nor a harness timeout occurred.

## Conclusions

- Supported and verified: alpha-renaming postcondition and nested quantifier binders preserves conversion output and successful verification.
- Expected proof failure: alpha-renaming the impossible postcondition binder preserves the verifier warning class and assertion text.
- Source diagnostic / verification skipped: existing invalid-locality and impure-specification controls behave as recorded.
- Harness behavior: repeated runs are stable; no-match exits 1; golden generation reports its observations; missing Z3 surfaces as a backend failure rather than being mistaken for a proof result.
- Timeout probe: the bounded `z_function` run verified within the limit in this environment.
- No unsupported-conversion exception, internal error, unexpected backend failure, or new defect was found.

## Final validation

- `./agent-scripts/check-all.sh`: Gradle `check` passed in 4m20s and `check-testdata.sh` passed. The wrapper exited 2 because `pre-commit` was unavailable.
- Attempted to install `pre-commit` in an isolated virtual environment; the package proxy rejected the download with HTTP 403.
- Ran all configured local hooks directly: `agent-scripts/tests/run.sh` passed all assertions, `agent-scripts/check-testdata.sh` passed, `git diff --check` passed, and every changed file ends with a newline (the configured `end-of-file-fixer` condition).
