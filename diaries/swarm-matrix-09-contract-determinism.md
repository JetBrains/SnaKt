# Swarm matrix 09: contract determinism diary

Assignment: GitHub issue #439, testing contract DSL and specification semantics
for deterministic, repeatable conversion, diagnostics, verification, exit status,
and golden output.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=5a7d1789-0521-494b-9a5e-143b7a9aa650

## Actions

- Read `AUTOMATIONS.md` before all repository exploration.
- Recorded the assignment in `automationsInstructions/swarm-matrix-09-contract-determinism.md`.
- Confirmed the checkout started on `implementing-air-automations` at `9bac7b3`.
- Inspected recent pull requests; `test/issue-<number>-<topic>` is the dominant branch convention.
- Located nearby precondition, postcondition, implication, quantifier, purity, and Kotlin contract tests with semantic code search.
- Searched open and closed GitHub issues for determinism/repeatability and contract-specification defects; no matching existing report was returned.
- Created branch `test/issue-439-contract-determinism` from the required source branch.
- Added `contract_determinism.kt`, pairing unchanged-semantics variants that exercise preconditions, postconditions, `verify`, `forAll`, `implies`, and propagation through calls. Added two equivalent expected proof failures as negative controls.
- Added `impure_contract_determinism.kt`, pairing equivalent impure `verify` expressions as source-diagnostic controls.
- The paired accepted specifications generate identical Viper clauses and bodies apart from function names. Their caller verifies equal results and propagated positivity.
- The two negative controls both classify as expected proof failures with the same Viper message. Parentheses move each diagnostic highlight to the enclosed expression but do not change its kind, ordering, or proof result.
- The impure controls both classify as source diagnostics: `VERIFICATION_SKIPPED` followed by `PURITY_VIOLATION`, in stable source order.
- A minimized malformed implication (`value >= 0 implies ...`) exposed phase-dependent failure handling. Conversion-only produced two `INTERNAL_ERROR` markers over the expression; full verification produced one `INTERNAL_ERROR` plus `NONE_APPLICABLE` and `UNRESOLVED_REFERENCE_WRONG_RECEIVER`. The parenthesized valid control converted and verified. This contradictory probe was not retained because one marker golden cannot pass both phases.
- Re-searched open and closed issues for the exact implication/internal-error behavior; found no report. Filed #496, “Malformed implication produces phase-dependent internal diagnostics,” with the minimal reproducer, expected/observed behavior, environment, control evidence, and `swarmTestingBug` label.

## Commands and results

- Initial fast loop under the preinstalled JBR 25.0.2: Gradle startup failed before tests. Installed a temporary Temurin 21.0.12.1 runtime to match CI.
- `./agent-scripts/test.sh <focused path>` for each retained case: passed after initial golden creation.
- `./agent-scripts/test.sh --verify <focused path>` for both retained cases, with Z3 4.8.7: passed. Accepted controls verified; the two `false` assertions produced the intended proof failures.
- `./agent-scripts/test.sh --update-goldens ...`: zero goldens rewritten. The report showed only the two intended proof-failure warnings and the expected conversion/source diagnostics; all were reviewed.
- Repeated conversion of `contract_determinism.kt`: passed with unchanged SHA-256 hashes for the conversion and verification goldens.
- `./agent-scripts/check-all.sh`: Gradle check and test-data checks passed; exit 2 because `pre-commit` was absent.
- Attempts to install `pre-commit` with pip and uv were blocked by the environment's PyPI proxy (HTTP 403). Ran the configured v5.0.0 end-of-file hook source over the new text files, then ran both local hooks directly: `check-testdata.sh` passed and `agent-scripts/tests/run.sh` passed all assertions. (The repository's excluded API dump and binary wrapper were restored after the raw hook was initially invoked without pre-commit's file filtering.)
