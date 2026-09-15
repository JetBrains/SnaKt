# Testing swarm agent 49 diary

## Assignment and setup

- Read `AUTOMATIONS.md` first and recorded issue #479 in `automationsInstructions/testing-swarm-agent-49.md`.
- Confirmed the checkout started at `implementing-air-automations` revision `9bac7b3` and created `swarm-testing/issue-479-determinism` from it.
- Read the golden-file and runner guidance in `docs/agents-dev.md` and `docs/developing.md`, then inspected nearby proof-failure and purity-diagnostic fixtures plus the test runner.
- The environment initially exposed JDK 25, which Gradle rejected before tests ran (`25.0.2`, exit 1). Installed Temurin JDK 21.0.12.1 under `/tmp` from the official GitHub release. Verification then identified the missing solver as a backend setup failure (`Cannot run prover at location 'z3': not a file`, exit 1). Installed the repository-documented Z3 4.8.7 release under `/tmp` and set `Z3_EXE` for subsequent runs.

## Added probe

Added `verification/contracts/deterministic_repetition.kt` with four `@AlwaysVerify` functions:

- two positive controls that assert equivalent truths, one with a harmless local variable;
- two negative controls that assert equivalent falsehoods, one with a harmless local variable.

The generated Viper preserves the intended differences. Both positive controls are supported and verified. Each negative control is an expected proof failure at its first `false` assertion. The later false arithmetic assertion is unreachable after `assert false`, so the absence of a second diagnostic in each method is expected.

`./agent-scripts/test.sh --update-goldens deterministic_repetition` reported and was reviewed in full. It generated the source markers, conversion golden, two expected verification warnings, and test registration. `./agent-scripts/check-testdata.sh` passed in the update report. A subsequent full verification run passed, confirming the observations were accepted correctly.

## Repeatability evidence

All runs used JDK 21.0.12.1 and Z3 4.8.7.

| Probe | Runs | Exit | Stable evidence | Classification |
| --- | ---: | ---: | --- | --- |
| `deterministic_repetition`, conversion | 2 | 0, 0 | FIR golden SHA-256 `311018311c2f5ff204b4ebf514adb7757bd478c14e901f6e4a71cb3c56816f0f` both times | supported conversion |
| `deterministic_repetition`, verify, `SILICON_PARALLEL_VERIFIERS=1` | 2 | 0, 0 | FIR hash above and verifier hash `3e7257d9d24008ed8655fd9beb35b2aa7be6fc059a4ce57247854a628d7a6357` both times | supported and verified plus expected proof failures |
| `deterministic_repetition`, verify, `SILICON_PARALLEL_VERIFIERS=2` | 1 | 0 | Same FIR and verifier hashes | stable backend worker-count perturbation |
| `assert_statements`, conversion | 2 | 0, 0 | FIR hash `2a8d41c4dfcca2a4808356c5f9a3386301e709b818d8ced90643bc4b8fcdbe28` both times | stable source diagnostics |
| `wrongly_annotated`, conversion | 2 | 0, 0 | FIR hash `2601f849821a4bc50b66d75780c837ad47decf25ae826157920dde15ed6ca699` both times | stable unsupported/blocked conversion diagnostics |
| nonexistent pattern | 1 | 1 | `test.sh` rejected the unmatched pattern | expected harness failure |
| externally bounded verification | 1 | 124 | GNU `timeout 1s` terminated the runner | timeout control; no native timeout diagnostic |
| verification without `Z3_EXE` | 1 | 1 | JUnit recorded `ExternalToolError` | backend failure propagated by harness |

Generated Viper, diagnostic ordering and text, proof outcomes, golden bytes, and successful runner exits were stable across unchanged repeats and the backend parallelism perturbation. No internal error occurred.

## Issue search and conclusion

Searched open and closed GitHub issues for determinism, flaky diagnostics, timeouts, and backend failures. Relevant existing reports include #283 (no native verification timeout), #383 (a Silicon exceptional abort can be treated as success), #363 (diagnostic determinism assignment), and #142 (Silicon close concurrency). The bounded external timeout confirms the lack of a harness-visible timeout outcome already tracked by #283; it is not a new defect. The missing-solver control failed loudly and consistently, unlike the exceptional-abort path already reported in #383.

No previously unreported defect was found, so no `swarmTestingBug` issue was opened.

## Final validation

- `./agent-scripts/check-all.sh`: Gradle `check` passed in 4m05s and `check-testdata.sh` passed. The first run returned 2 because `pre-commit` was absent.
- Downloaded the official pre-commit 4.6.0 zipapp and reran. Gradle `check` and `check-testdata.sh` passed again, but pre-commit could not create the third-party hook environment because the workspace proxy returned 403 for the required `ruamel.yaml`/PyPI dependencies.
- Ran every configured hook directly: `end_of_file_fixer.py` over all changed files, `agent-scripts/check-testdata.sh`, and `agent-scripts/tests/run.sh`; all returned 0. Thus all configured hook behavior relevant to this change passed, although the pre-commit wrapper could not finish dependency setup.
- `git diff --check` passed.

Produced by Air Automations. Name: Testing swarm agent 49 / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=3ca001e0-d0d8-4300-9340-16261a23e63a
