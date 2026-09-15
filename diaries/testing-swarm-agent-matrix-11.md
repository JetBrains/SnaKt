# Testing swarm agent — matrix 11 diary

## 2026-09-15 — Issue #441

### Assignment

- Read `AUTOMATIONS.md` before taking any other repository action.
- Recorded the issue #441 assignment in `automationsInstructions/testing-swarm-agent-matrix-11.md`.
- Created `test/issue-441-heap-aliasing` from `implementing-air-automations`, following recent testing-branch conventions.
- Scope: heap objects, aliasing, and mutation under boundary-value analysis.

### Investigation and evidence

- Inspected the existing class-verification and uniqueness suites before choosing probes. The closest coverage was `classes/primary_constructors.kt`, `classes/property_accessors.kt`, `classes/unique_fields.kt`, `uniqueness_checker/aliasing.kt`, `uniqueness_checker/nullable.kt`, and `uniqueness_checker/constructor.kt`.
- Added `classes/heap_aliasing_boundaries.kt` with bounded partitions for constructor values immediately below, at, and above zero; `Int.MIN_VALUE` and `Int.MAX_VALUE`; nullable empty and singleton states; unique and shared aliases; and nullable field replacement with empty and singleton values.
- The first `./agent-scripts/test.sh heap_aliasing_boundaries` attempt was a harness failure under the environment's Java 25.0.2. Downloaded Temurin 21.0.12.1 from the official Adoptium GitHub release and reran under that supported JDK.
- The fast conversion loop then exposed `INTERNAL_ERROR` on the `Int` qualifier in both `Int.MIN_VALUE` and `Int.MAX_VALUE`. The neighbouring `-1`, `0`, and `1` constructor controls converted successfully.
- Searched open and closed GitHub issues for both constants and resolved qualifiers. JetBrains/SnaKt#247 already documents the same `Unsupported resolved qualifier FirRegularClassSymbol` outcome; it was closed after moving tracking to komiputer/SnaKt#66. No duplicate issue was filed.
- Installed the repository-required Z3 4.8.7 binary from the official Z3 GitHub release after the first verification attempt correctly classified the missing executable as a harness failure.
- Read every generated golden observation. Constructor contracts for `-1`, `0`, and `1` retain unique permission and exact field-value postconditions. Nullable empty and singleton functions retain uniqueness conditionally and verify exact nullness. These five cases are supported and verified.
- Unique-alias mutation, shared-alias mutation observed through the original reference, and nullable empty/singleton field replacement convert, but default property accesses are reduced to unconstrained values or omit the update. Their precise postconditions therefore produce expected proof failures. These are recorded as negative controls rather than reported as defects because property mutation reasoning is incomplete and the assignment explicitly says a missing feature is not automatically a bug.
- `./agent-scripts/test.sh --verify heap_aliasing_boundaries` passed: one test, with all expected conversion, internal-error, and verification-error goldens matching.
- `./agent-scripts/check-all.sh` ran the complete Gradle check and test-data validation successfully. It returned exit 2 only because `pre-commit` was unavailable. A system/user install was blocked by the externally managed Python environment, and an isolated virtual-environment install was blocked by the package proxy with HTTP 403.

### Conclusions

- Supported and verified: unique construction and exact field-value contracts at `-1`, `0`, and `1`; unique nullable empty and singleton returns.
- Expected proof failure: mutation observed through unique/shared aliases and nullable field replacement, because current generated accessor semantics do not establish the post-update heap fact.
- Internal error, already reported: `Int.MIN_VALUE` and `Int.MAX_VALUE` through a class-qualified constant reference (JetBrains/SnaKt#247, moved to komiputer/SnaKt#66).
- Harness failures resolved: unsupported Java 25 and missing Z3. Remaining proportional-check gap: `pre-commit` could not be installed through the environment proxy; Gradle check and test-data checks passed.
- No new defect issue was warranted.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=c7f36e76-884b-476a-ab08-f82ba0e9fee3
