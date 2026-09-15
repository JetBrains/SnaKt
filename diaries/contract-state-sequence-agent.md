# Contract state-sequence agent diary

## Assignment

- Trigger: [issue #437](https://github.com/JetBrains/SnaKt/issues/437)
- Area: Contract DSL and specification semantics
- Method: state-transition sequence testing
- Required controls: at least one positive control and one negative or boundary control

## Actions

1. Read `AUTOMATIONS.md` before taking any other repository action.
2. Confirmed the checkout was clean and on `implementing-air-automations` at `9bac7b3`.
3. Inspected recent pull requests targeting `implementing-air-automations`; selected the established `test/issue-...` branch convention.
4. Confirmed GitHub authentication is available for delivery and issue research.
5. Mapped the DSL implementation and nearby fixtures. `ContractBuilder` conjoins collected preconditions and postconditions, while `ProgramConverter` propagates user contracts to call sites. Existing coverage proves a single producer-to-consumer hop but not a longer round trip.
6. Searched open and closed GitHub issues for contract propagation, sequence, quantifier, implication, and purity terms. Found the related swarm matrix assignments but no pre-existing defect matching the planned sequence probes.
7. Added `contract_state_sequences.kt` with:
   - a four-transition `0 -> 1 -> 2 -> 1 -> 0` round trip, checking facts after every call;
   - a shorter positive round-trip control;
   - quantified implication pre/postconditions propagated through a call;
   - an upper-boundary precondition violation as an expected proof failure;
   - a mutating `verify` expression as an expected purity source diagnostic.
8. The first test command failed before compilation because the environment supplied JDK 25.0.2, which Gradle 8.14.3 could not parse. Installed a temporary Temurin 21.0.12.1 toolchain, matching the project's JVM 21 target; conversion then ran successfully.
9. The first verification command reached the backend but reported a harness failure because Z3 was absent. Installed the documented Z3 4.8.7 release temporarily, set `Z3_EXE`, stopped any captured Gradle daemon, and reran.
10. Initial verification classified the upper-boundary call as the expected proof failure. It also could not prove the final `state == 0` solely by instantiating an untriggered quantified postcondition. This was a probe-design weakness rather than a contradicted semantic guarantee, so `quantifiedEcho` was minimized to include the direct identity fact alongside its quantified implication.

## Golden review and classifications

- `contractStepUp` and `contractStepDown`: **supported and verified**. Conversion conjoined both preconditions and both postconditions for each operation.
- `roundTripSequence`: **supported and verified**. Calls propagated their contracts through `0 -> 1 -> 2 -> 1 -> 0`; every assertion after every transition passed. The final quantified echo also preserved the restored state.
- `shortRoundTripControl`: **supported and verified**. The shorter `0 -> 1 -> 0` ordering passed.
- `quantifiedEcho`: **supported and verified**. Universal quantifiers and implications were converted in both precondition and postcondition, and the direct identity fact propagated to the caller.
- `upperBoundaryViolation`: **expected proof failure**. Z3 reported exactly the false callee obligation `2 < 2`; the diagnostic is attached to the call.
- `impureVerifySequence`: **source diagnostic**. Mutation inside `verify` produced `PURITY_VIOLATION` and skipped verification of that function.
- No unsupported conversion, internal error, backend failure, timeout, or product-level harness failure remained after supplying the documented JDK and Z3 dependencies.

## Commands and evidence

- `./agent-scripts/test.sh contract_state_sequences` initially exposed the unsupported active JDK; with Temurin 21 it passed conversion (`1 passed`).
- `./agent-scripts/test.sh --verify contract_state_sequences` initially exposed missing Z3; with documented Z3 4.8.7 it produced the first proof observations.
- `./agent-scripts/test.sh --update-goldens contract_state_sequences` recorded and reported the generated Viper and the single expected proof failure. Every reported golden was read in full and matched the intended cases.
- The final `./agent-scripts/test.sh --verify contract_state_sequences` passed (`1 passed`).
- `./agent-scripts/check-all.sh`: Gradle `check` passed and `check-testdata.sh` passed. The first run returned exit 2 because `pre-commit` was absent. A standalone `pre-commit` 4.6.2 executable was then supplied, but its `pre-commit-hooks` environment failed to bootstrap because the automation proxy returned HTTP 403 for PyPI's setuptools package; the rerun therefore reported the pre-commit stage failed. The two local hooks were run directly: `check-testdata.sh` passed and `agent-scripts/tests/run.sh` passed all assertions. `git diff --check` also passed, covering whitespace while the configured end-of-file hook could not bootstrap.

## Conclusion

The bounded sequences confirm contract propagation, conjunction, intermediate `verify` facts, quantifier/implication conversion, and purity enforcement for the tested forms. No previously unreported bug was confirmed, so no `swarmTestingBug` issue was filed.

## Delivery

- Committed the test and diary as `57ff88c` on `test/issue-437-contract-state-sequences` and pushed the branch.
- Opened [pull request #504](https://github.com/JetBrains/SnaKt/pull/504) against `implementing-air-automations`.
- Added the required `swarmTestingDone` label to trigger issue #437 and verified both `swarmTesting` and `swarmTestingDone` are present.
