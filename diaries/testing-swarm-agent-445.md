# Testing swarm agent 445 diary

## Assignment

Issue #445 assigns bounded model-based oracle testing of heap objects, aliasing, and mutation. Required coverage includes fields, constructors, getters and setters, unique ownership, aliases, nullable references, and updates observed through multiple references, with positive and negative or boundary controls.

## Actions

- Read `AUTOMATIONS.md` before all other repository investigation.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-445.md`.
- Confirmed the checkout started on `implementing-air-automations` with no pre-existing changes.
- Inferred the `test/issue-<number>-<topic>` branch convention from recent pull requests and created `test/issue-445-heap-aliasing` from `implementing-air-automations`.
- Inspected nearby constructor, property accessor, uniqueness aliasing, permission, and backing-field verification tests. Existing conversion coverage models constructor fields and uniqueness predicates, but no ordinary verification case compared writes and reads through aliases.
- Defined an independent bounded oracle over integer states `{-1, 0, 1}`. Added constructor observations for all three states, transitions `-1 -> 0` and `0 -> 1`, shared and nullable aliases, explicit unique ownership, a custom getter/setter, a true baseline, and a deliberately false mutation control.
- The host JDK 25.0.2 could not configure the Gradle build (`25.0.2`). Installed Temurin 21.0.12.1 under `/tmp` and used it for all subsequent commands.
- Conversion (`./agent-scripts/test.sh heap_aliasing_model`) succeeded. Generated Viper preserves reference identity but lowers field reads to unconstrained `havoc` values and emits no usable heap update facts for these clients.
- Installed the repository-documented Z3 4.8.7 under `/tmp` after the first `--verify` run correctly failed as a backend/environment failure (`Cannot run prover at location 'z3': not a file`).
- Full verification classified the plain `verify(0 == 0)` and alias identity assertions as **supported and verified**. Constructor-value assertions for `-1`, `0`, and `1`; direct mutations; shared-alias observation; nullable smart-cast alias observation; explicit `@Unique` mutation; and the deliberately false control all produced **expected proof failures**. This is conservative: none of the runtime-true heap facts was falsely accepted.
- The custom getter/setter probe produced source diagnostics: **purity violation** on property reads in `verify`, with verification skipped for that function.
- A minimized nullable assignment receiver, `cell!!.value = 1`, produced an **internal error** for `FirCheckNotNullCallImpl`. Repeated runs also changed the embedded FIR identity (`@43df1377` then `@63f2d024`), making an internal-error golden unstable. Removed that unstable fixture from the branch after preserving the reproducer and evidence in bug #494.
- Searched open and closed GitHub issues using `alias mutation heap object property setter getter`, `backing field`, `property access verification`, `class field mutation`, `FirCheckNotNullCallImpl`, and `check not null conversion`. No existing report covered the non-null assertion assignment receiver failure.
- Opened #494, **Non-null assertion receiver in property assignment reports unstable INTERNAL_ERROR**, with the required `swarmTestingBug` label, minimal reproducer, expected/observed behavior, revision/tool versions, and control evidence.
- Audited `--update-goldens` output. The nine Viper diagnostics correspond exactly to the nine marked expected proof failures; the generated conversion text records the unconstrained field reads; testData checks passed. The unstable internal-error golden was intentionally excluded.
- Final targeted checks: conversion and `--verify` each ran one test and passed.
- `./agent-scripts/check-all.sh` completed `gradle check` and `check-testdata.sh` successfully, then exited 2 because `pre-commit` was unavailable. Installation attempts with both pip in a temporary virtual environment and uv were blocked by the environment proxy (403 from `files.pythonhosted.org`; direct networking had no DNS).
- Ran the available local pre-commit hooks directly: `agent-scripts/tests/run.sh` passed all assertions, `agent-scripts/check-testdata.sh` passed, and `git diff --check` passed. The external `end-of-file-fixer` hook could not be installed; all added text files were manually reviewed for final newlines.
- Committed and pushed the test branch, opened PR #532 against `implementing-air-automations`, and verified its base/head and open state.
- Added the required `swarmTestingDone` label to triggering issue #445 and verified that both `swarmTesting` and `swarmTestingDone` are present.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=90d3345e-d4f8-4f4b-83b5-972c58b68485
