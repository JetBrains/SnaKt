# Testing swarm agent 20 diary

Assignment: [issue #450](https://github.com/JetBrains/SnaKt/issues/450), heap objects, aliasing, and mutation using coverage-guided feature composition.

## Actions

- Read `AUTOMATIONS.md` before taking any other repository action.
- Confirmed the checkout is clean and on `implementing-air-automations`, as required.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-20.md`.
- Inspected recent pull requests targeting `implementing-air-automations`; current testing branches conventionally use `test/issue-<number>-<topic>` or `test/swarm-<number>-<topic>`.
- Used semantic code search to locate existing heap-related coverage. The closest tests cover primary constructors, unique fields, backing-field getters, heap-dependent pure expressions, and side-effecting field access.
- Created branch `test/issue-450-heap-aliasing`, following the recent `test/issue-<number>-<topic>` convention.
- Chose a minimal unique mutable cell seed. Added features one at a time: constructor getter observation, direct setter/getter mutation, a second local alias, nullable alias refinement, and a deliberately stale-value assertion as the negative boundary control.
- The first fast-loop conversion passed after generating its new FIR golden. Reading the entire golden showed automatic property reads lowered to unconstrained `havoc` values, despite the constructor returning a folded unique predicate containing the field value.
- The first verification attempt was a harness failure because Z3 was absent. Installed the repository-documented Z3 4.8.7 locally and reran with `Z3_EXE` set.
- Verification then classified all five automatic-access cases as expected proof failures. This matches the nearby `negative/linked_list.kt` documentation that constructor expressions currently do not verify due to missing uniqueness information, so it is not a newly reportable defect.
- Added explicit `@Manual` predicate controls to test the supported path: constructor read, mutation observed through an alias, and the same composition through a nullable alias. Each unfolds before access and refolds afterward.
- Ran `./agent-scripts/test.sh --update-goldens heap_aliasing` with JDK 21 and Z3 4.8.7. Read all reported changes: the Viper diagnostic golden contains exactly the five intended automatic-access proof failures; the FIR golden shows `havoc` for those accesses and concrete field reads/writes for all explicit-predicate controls. The generated test registration changed as expected. Test-data consistency passed.
- Ran `./agent-scripts/test.sh --verify heap_aliasing`; the full focused pipeline passed (1 test).
- Searched open and closed GitHub issues for uniqueness, constructors, fields, and verification. Issue #305 discusses the broader missing class-invariant abstraction; no evidence contradicted the already documented automatic uniqueness limitation, so no `swarmTestingBug` issue was filed.

## Outcome classification

- Supported and verified: explicit unique-predicate constructor read; setter update observed through a second non-null reference; setter update observed through a nullable alias after refinement.
- Expected proof failure: automatic folded-predicate getter after construction; automatic mutation/readback; automatic observation through non-null and nullable aliases.
- Negative boundary control: a stale pre-mutation value after mutation correctly fails verification.
- No source diagnostic, unsupported conversion, internal error, backend failure, timeout, or remaining harness failure.

## Final checks

- `./agent-scripts/check-all.sh`: clean exit 0 after installing the missing pre-commit runner in a temporary environment. Gradle checks, test-data checks, end-of-file checks, and script tests all passed.
