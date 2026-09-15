# Testing swarm agent — matrix 14 diary

## Assignment

Issue [#444](https://github.com/JetBrains/SnaKt/issues/444) requested compiled-Kotlin differential probes for heap objects, aliasing, mutation, fields, constructors, accessors, unique ownership, and nullable references. Work started from revision `9bac7b3` on `implementing-air-automations` and continued on `test/issue-444-heap-alias-mutation`.

## Probes and evidence

Added `heap_alias_mutation.kt` with these bounded cases:

- `aliasIdentityControl`: positive control. A local alias is referentially identical to its `@Unique` parameter. SnaKt verifies `alias === cell`.
- `constructorAndDirectMutation`: constructs a cell, writes `2`, then reads it. Compiled Kotlin produces `2`; SnaKt reports an expected proof failure because the read is lowered to fresh `havoc` and the write is absent.
- `mutationThroughAlias`: writes `2` through an alias and reads through the original reference. Compiled Kotlin produces `2`; SnaKt reports the same expected proof failure.
- `staleValueAfterAliasMutation`: negative control. It writes `1`, overwrites with `2` through an alias, then asserts the stale value `1`. Compiled Kotlin confirms the assertion is false; SnaKt also rejects it with an expected proof failure.
- `nullableAliasMutation`: writes `3` through a safe call on a nullable alias after the original reference is checked non-null. Compiled Kotlin produces `3`; SnaKt reports an expected proof failure because the final property read is `havoc`.
- `customAccessorBoundary`: the setter stores `newValue + 1` and the getter returns the backing field plus one, so compiled Kotlin returns `6` after assigning `4`. SnaKt classifies the function as unsupported conversion: verification is skipped because the accessor read in `verify` is impure.

The ordinary Kotlin oracle was compiled with Kotlin 2.3.0 and run on Temurin 17.0.20.1. Its checked output was:

```text
identity=true direct=2 alias=2 stale=false nullable=3 accessor=6
```

No SnaKt successful verification claim contradicted compiled Kotlin. The only successful heap-related claim was alias identity, which the runtime control confirmed.

## Commands and results

- `./agent-scripts/test.sh heap_alias_mutation`: conversion passed after accepting the expected unsupported custom-accessor diagnostics.
- `./agent-scripts/test.sh --update-goldens heap_alias_mutation`: recorded four `Assert might fail` diagnostics. Each matches the intended outcomes above; the positive runtime mutations fail because mutable property reads/writes are not modeled, and the stale negative control fails correctly.
- `./agent-scripts/test.sh --verify heap_alias_mutation`: passed with the reviewed goldens.
- Compiled and ran the equivalent bounded program with Kotlin 2.3.0: all runtime checks passed and produced the output recorded above.
- Searched open and closed GitHub issues for property, field, alias, unique, mutation, and havoc behavior. Existing issue [#377](https://github.com/JetBrains/SnaKt/issues/377), “Mutable property reads become havoc and writes are omitted,” exactly covers the observed limitation. No duplicate bug was filed.
- `./agent-scripts/check-all.sh`: Gradle check and test-data checks passed. Its pre-commit stage could not provision `pre-commit-hooks` because the environment proxy returned 403 for `files.pythonhosted.org`; installing pre-commit itself from its official release succeeded, but hook dependency installation hit the same external block. The configured checks were completed directly: `git diff --check`, final-newline checks, `./agent-scripts/check-testdata.sh`, and `./agent-scripts/tests/run.sh` all passed.

## Conclusion

The probes add differential regression coverage for the known mutable-property limitation and confirm that it fails closed: valid runtime heap-value assertions are not proven, while the false stale-value assertion is also rejected. Alias identity is supported and verified. Custom accessors remain unsupported for verification. No new defect was found.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=c26df68f-dd75-4ac0-a51b-0b8ffd4e8813
