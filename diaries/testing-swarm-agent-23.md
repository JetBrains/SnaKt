# Testing swarm agent 23 diary

- Read `AUTOMATIONS.md` before taking any repository action.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-23.md`.
- Inspected the existing generic conversion test, verifier contract conventions, test tooling documentation, and recent branches/PRs.
- Searched open and closed GitHub issues for generic-instantiation defects. Issue #312 already tracks erased generic arguments, substituted contracts, bounds, nullability, and possible cross-instantiation leakage.
- Added a focused golden testData probe covering distinct `Int`/`Boolean` field and method instantiations, a nullable type argument, and a `Number`-bounded type parameter. Every positive contract has an intentionally false near-boundary control.
- Installed a temporary Temurin JDK 21 after the environment's default JDK 25 failed during Gradle Kotlin DSL evaluation, then generated the test runner.
- The first conversion run reached the new test and requested `VIPER_TEXT` expectations for both generic member methods; added those expected diagnostic markers.
- Installed the required Z3 4.8.7 temporarily after full verification initially reported that `z3` was unavailable.
- Read the complete golden-update report. Conversion specializes generic method call signatures independently for `Boolean` and `Int`, but generic member bodies erase parameters to `nullable(anyType())`; generic field reads are havoced. The bounded member similarly loses its `Number` bound in its standalone body.
- Full verification rejected every exact-value positive contract and every intentionally false near-boundary contract. This classifies the surface as a proof limitation rather than fact leakage: incompatible instantiations did not make any false claim verify. The limitation is already covered by issue #312, so no duplicate bug issue was opened.
- Re-ran the focused conversion test and focused full-pipeline test after recording goldens; both passed (1/1).
- Ran `agent-scripts/check-all.sh`: Gradle `check` and testData checks passed, but the wrapper returned exit 2 because `pre-commit` was absent. Installing `pre-commit` was blocked by the environment proxy (HTTP 403 from files.pythonhosted.org).
- Ran the configured hooks directly: script tests passed, testData checks passed, all changed files end with a newline, and `git diff --check` passed. No repository-wide check failed; only the unavailable `pre-commit` runner prevented a clean wrapper exit.
