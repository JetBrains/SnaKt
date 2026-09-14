# Testing swarm agent 34 diary

- Read `AUTOMATIONS.md` before taking any repository action.
- Read the assignment in issue [#351](https://github.com/JetBrains/SnaKt/issues/351).
- Confirmed the checkout was clean and on `implementing-air-automations`, with its matching remote branch.
- Inspected recent commits and pull requests targeting the automation branch to establish commit and branch naming conventions.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-34.md` and started this diary as required.
- Created branch `test/swarm-34-shadowing` from `implementing-air-automations`.
- Used semantic code search to locate existing shadowing, receiver, field, contract, and golden-test examples.
- Read the existing `shadowing.kt` conversion test, its complete FIR golden, receiver and field examples, negative verification annotations, and `docs/agents-dev.md`.
- Added `shadowing_values.kt` with contract-observable distinct values covering parameter/local/nested-local shadowing and dispatch receiver/extension receiver/field/parameter/local resolution. Included successful assertions and deliberately false boundary assertions marked as expected verification failures.
- The initial test attempt stopped before running because the environment defaulted to JDK 25. Located the bundled JDK 21.0.11 runtime specified by CI and used it for all subsequent checks.
- Minimized an initial `java.io.Serializable` conversion error and determined it was caused by placing the required `FULL_JDK` test directive incorrectly, rather than by a SnaKt defect. Moving the directive to the file header resolved it.
- Installed CI's Z3 4.8.7 release through authenticated GitHub tooling after discovering that verification could not run without a prover.
- Ran the conversion-only test repeatedly while minimizing and refining the probes. Inspected the complete generated Viper diff and confirmed distinct symbols for the parameter, both nested locals, dispatch receiver, extension receiver, and field getter.
- Ran `--update-goldens` and read its complete report. Confirmed the two recorded verification diagnostics are the deliberately false negative controls, while the successful assertions and `112337` postcondition are retained as positive controls.
- Reran `./agent-scripts/test.sh shadowing_values`: 1 test passed.
- Ran `./agent-scripts/test.sh --verify shadowing_values`: 1 test passed, including verification.
- Concluded that the tested shadowing and name-resolution surface behaves correctly. No new bug was found, so no bug report was opened.
- Ran `./agent-scripts/check-all.sh`; Gradle and testData checks passed, but the command returned exit 2 because `pre-commit` was not installed.
- Installed the official pre-commit 4.0.1 zipapp and prepared its hook environment offline because the environment proxy blocked PyPI build dependencies. Ran all configured hooks successfully.
- Reran `./agent-scripts/check-all.sh`: Gradle check, testData checks, end-of-file fixer, script tests, and all pre-commit hooks passed with exit 0.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=638c282d-3ac8-45d8-a38c-c48ab13212a8
