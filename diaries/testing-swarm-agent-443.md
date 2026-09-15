# Testing swarm agent 443 diary

## Assignment

Test heap objects, aliasing, and mutation using contract mutation and negative controls, as specified in GitHub issue #443.

## Actions and evidence

- Read `AUTOMATIONS.md` before taking any repository action.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-443.md` as required.
- Confirmed the checkout started on `implementing-air-automations`, reviewed recent PR branch names, and created `test/issue-443-heap-alias-mutation` from that source branch.
- Inspected existing constructor, property accessor, unique-field, nullable, and uniqueness-alias tests before selecting probes.
- Searched open and closed GitHub issues for field mutation and alias behavior. Issue #377, "Mutable property reads become havoc and writes are omitted," already records the mutable-property limitation reproduced here; no new bug issue was filed.
- Added `heap_alias_contract_mutation.kt` with six bounded probes:
  - `constructorAliasPositiveControl`: constructing an immutable field with 7 and reading it through an alias is supported and verified.
  - `constructorAliasMutatedControl`: changing only the constructor argument from 7 to 8 produces the expected proof failure for `alias.value == 7`.
  - `mutableAliasAssignmentBoundary`: assigning 7 through an alias cannot establish either the immediate assertion or the postcondition; conversion omits the write and lowers the read to `havoc`, matching #377. Classified as expected proof failure caused by a known unsupported conversion boundary.
  - `mutableAliasContractNegative`: assigning 8 instead of 7 produces the expected postcondition failure. The omitted write is visible in the conversion golden.
  - `nullableAliasAssignmentBoundary`: on the non-null branch, assigning 11 through the alias does not establish the nullable postcondition because the write is omitted. Classified as expected proof failure at the same known boundary.
  - `nullableAliasContractNegative`: changing the assigned value to 12 preserves the expected postcondition failure.
- The first fast-loop attempt was a harness failure before compilation because the host Java 25 runtime is unsupported by the Gradle Kotlin DSL version. Located the bundled Java 21.0.11 runtime and reran with it.
- The first verification attempt was a harness failure because Z3 was absent. Downloaded the repository-documented Z3 4.8.7 release to a temporary directory, set `Z3_EXE`, stopped the captured Gradle daemon, and reran.
- Ran `./agent-scripts/test.sh heap_alias_contract_mutation`: conversion passed after the initial golden was generated.
- Ran `./agent-scripts/test.sh --update-goldens heap_alias_contract_mutation` and read all 145 conversion-golden lines, all 9 verification-diagnostic lines, and the updated source markers. The recorded Viper shows constructor argument facts for the controls, omitted mutable writes, a fresh `havoc` mutable read, and the intended assertion/postcondition diagnostics.
- Ran `./agent-scripts/test.sh --verify heap_alias_contract_mutation`: 1 test passed, 0 failed.
- Ran `./agent-scripts/check-all.sh`: Gradle check and test-data checks passed; the command returned exit 2 only because `pre-commit` was unavailable.
- Attempted to install `pre-commit` in an isolated temporary virtual environment, but the package download was blocked by the environment proxy (HTTP 403).
- Ran the configured hooks directly instead: the v5.0.0 `end-of-file-fixer` implementation, `agent-scripts/check-testdata.sh`, and `agent-scripts/tests/run.sh`. Test-data checks and all script assertions passed; no repository text file needed an end-of-file change.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=b96ae2c3-acdd-49ac-9f4c-bbf2e5670bd3
