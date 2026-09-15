# Testing swarm agent 455 diary

## Assignment

- Trigger: [issue #455](https://github.com/JetBrains/SnaKt/issues/455)
- Area: collections, arrays, and indexing.
- Method: model-based oracle testing over a bounded domain.
- Run: [Testing swarm agent](https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=4f7b29ac-7897-4ea0-b3c0-c2c3036952ce)

## Protocol and setup

- Read `AUTOMATIONS.md` before repository investigation.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-455.md`.
- Confirmed the checkout started on `implementing-air-automations` at `9bac7b3`.
- Inspected recent pull requests targeting that branch and followed the established `test/swarm-<issue>-<topic>` convention.
- Created branch `test/swarm-455-collections-model-oracle` from the required source branch.
- Confirmed GitHub authentication is available as `jetbrains-air[bot]`.

## Investigation, probes, and evidence

- Delegated a read-only semantic exploration of collection and array models and nearby goldens. Confirmed `StdLibConverter` models `Collection`, `List`, and `MutableList` operations (`emptyList`, `isEmpty`, `get`, `add`, and `subList`) but contains no native-array or indexed-write model. Array `size` and `get` fall through to generic contracts.
- Inspected existing list goldens and negative bounds tests before designing probes.
- Searched open and closed GitHub issues for collections, arrays, indexing, bounds, primitive arrays, and writes. Relevant prior reports are:
  - [#379](https://github.com/JetBrains/SnaKt/issues/379): `List.size` in preconditions can cause an internal error.
  - [#381](https://github.com/JetBrains/SnaKt/issues/381): `List.first`/`last` lack nonempty preconditions.
  - [#482](https://github.com/JetBrains/SnaKt/issues/482): native-array reads verify without bounds obligations.
  - [#252](https://github.com/JetBrains/SnaKt/issues/252): indexed writes are unsupported and produce an internal error.
  - [#253](https://github.com/JetBrains/SnaKt/issues/253): native-array size lacks a non-negativity invariant.
- Defined the bounded oracle over abstract sequence length `n` and candidate indices `{-1, 0, n}`:

  | State | Index | Oracle | Observed classification |
  | --- | ---: | --- | --- |
  | empty list (`n = 0`) | `0` | invalid | expected proof failure (`POSSIBLE_INDEX_OUT_OF_BOUND`) |
  | singleton from `listOf` (`n = 1`) | `0` | valid | expected proof failure because singleton size is unmodeled |
  | nonempty list (`n > 0`) | `0` | valid | supported and verified after `isEmpty` refinement |
  | arbitrary list (`n >= 0`) | `-1` | invalid | expected proof failure |
  | arbitrary list (`n >= 0`) | `n` | invalid | expected proof failure |
  | mutable list after `add` (`n -> n + 1`) | old `n` | valid | supported and verified |
  | mutable list after `add` (`n -> n + 1`) | new `n + 1` | invalid | expected proof failure |
  | empty/reference array | `0` | invalid | verified without diagnostics; known defect #482 |
  | reference array | `-1` | invalid | verified without diagnostics; known defect #482 |
  | empty primitive array | `0` | invalid | verified without diagnostics; same missing model as #482 |
  | primitive array | `size` | invalid | verified without diagnostics; same missing model as #482 |

- Added deterministic golden cases for list and mutable-list size/index obligations, singleton behavior, reference arrays, and `IntArray`.
- Probed `values[0] = 7` for both `Array<Int>` and `IntArray`. Conversion produced `INTERNAL_ERROR` with `Not yet implemented for ... FirUnitExpression`, matching #252. The diagnostic includes a changing object identity, so retaining it would make the golden nondeterministic; removed those two source cases after recording the evidence.
- Installed run-local Temurin JDK 21.0.12.1 and Z3 4.8.7 after the environment's JBR 25 failed Gradle configuration and no `z3` binary was present. Both are under `/tmp` and are not repository changes.
- Read all golden-update output. The five list verification diagnostics exactly match the oracle's expected failures: empty index zero, unmodeled singleton index zero, negative one, size, and post-add new size. The array cases intentionally have no Viper diagnostic, recording the already-reported missing bounds model.
- Focused validation passed:
  - `./agent-scripts/test.sh model_based_oracle`
  - `./agent-scripts/test.sh --verify model_based_oracle`
  - `./agent-scripts/test.sh array_model_probe`
  - `./agent-scripts/test.sh --verify array_model_probe`
- `./agent-scripts/check-all.sh` completed Gradle's full `check` and the testData checks successfully. It returned exit 2 only because `pre-commit` was unavailable. A run-local installation attempt was blocked by the environment proxy (`files.pythonhosted.org` returned 403), so the configured local hooks were run directly: `agent-scripts/check-testdata.sh` and `agent-scripts/tests/run.sh` passed. `git diff --check` and an explicit final-newline check over changed files also passed, covering the configured `end-of-file-fixer` requirement without mutating files.
- No new issue was filed because every contradicted semantic or failure-handling behavior was already reported.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=4f7b29ac-7897-4ea0-b3c0-c2c3036952ce
