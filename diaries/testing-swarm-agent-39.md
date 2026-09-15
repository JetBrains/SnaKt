# Testing swarm agent 39 diary

## 2026-09-15 — Issue #469

### Assignment

- Area: control flow, calls, recursion, and evaluation order.
- Method: determinism and repeatability testing with unchanged reruns and harmlessly perturbed equivalents.
- Required controls: at least one positive and one negative or boundary case.

### Actions and evidence

- Read `AUTOMATIONS.md` before investigating the repository.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-39.md`.
- Confirmed the checkout was clean on `implementing-air-automations` at `9bac7b3`.
- Reviewed recent pull-request branch names and created `test/issue-469-control-flow-determinism` from the required source branch.
- Inspected existing control-flow fixtures for nested calls, recursion, expression side effects, `if`, `when`, loops, and early returns, plus the verification diagnostic collector and harness conventions.
- Added `deterministic_calls.kt` and regenerated its test-runner registration with Java 21 after the environment's default Java 25 runtime failed Gradle configuration.
- Ran `./agent-scripts/test.sh --update-goldens deterministic_calls` and read the complete generated conversion and verification goldens. The update first created the conversion golden, then created the verification golden on the next full run, as expected for a new fixture.
- Full verification initially reported a backend setup failure because `z3` was absent. Ubuntu package indexes and PyPI were unavailable through the environment network, so downloaded the official Z3 5.1.0 Linux release through the authenticated GitHub API into `/tmp` and placed its binary on `PATH` for verification.
- Ran `./agent-scripts/test.sh --verify deterministic_calls` twice unchanged with Java 21 and Z3 5.1.0; both runs passed with one test, zero failures, and exit status 0.
- Before and after the repeated final verification run, the conversion golden SHA-256 remained `9a3cca8b43fbd64580096218c2760aff9218e2ca09f634e2ce4beae9cd9da058`, and the verification golden remained `70530e6c49118e51a93518bea69130c1a7574a26ec0745b0f77f2388c0237b17`.
- Ran `./agent-scripts/check-all.sh` with Java 21 and Z3 5.1.0. Test-data checks passed. The Gradle check ran 143 compiler-plugin tests and failed only the pre-existing `verification/user_invariants/exists.kt` golden comparison; the new `deterministic_calls` test passed in the same run. `pre-commit` was skipped because it is not installed, and the package proxy prevented installation. Per the check script contract, the overall check therefore did not pass.
- Ran `git diff --check`; it passed.
- Re-ran the fast conversion loop with `./agent-scripts/test.sh deterministic_calls`; one test passed with zero failures.

### Probes and conclusions

- `nestedCallsBaseline` and the harmlessly perturbed `nestedCallsPerturbed` are supported conversions. Their Viper output preserves left-to-right nested-call evaluation and stable local allocation; repeated runs produced byte-identical output.
- `recursiveBoundary` is a supported conversion. Its early-return base case precedes the recursive call, and the nested outer call remains ordered after recursion in generated Viper.
- `positiveControl` is supported and verified: both branches constrain the result to 1 or 2, and its assertion produced no verification diagnostic.
- `negativeDiagnosticFirst` and `negativeDiagnosticSecond` are expected proof failures. Their distinct assertions exercise stable source-order and source-location reporting in the verification golden.
- No diagnostic-order, source-location, proof-result, exit-status, or golden-output instability was observed. No internal error, unsupported conversion, timeout, or harness failure remains in the completed run. The missing default solver was an environment backend setup failure and was resolved locally.
- No product defect was confirmed, so no bug issue was filed.

Produced by Air Automations. Name: Testing swarm agent 39 / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=c5a9a2e7-80e7-402b-ab13-a9448c034458
