# Heap aliasing testing agent diary

## Assignment

Issue: [#446 — Heap objects, aliasing, and mutation × Pairwise feature interaction testing](https://github.com/JetBrains/SnaKt/issues/446)

Branch: `test/issue-446-heap-aliasing-mutation`, based on `implementing-air-automations`.

The assignment was recorded in `automationsInstructions/heap-aliasing-testing-agent.md` before implementation work.

## Exploration

- Read `AUTOMATIONS.md`, `AGENTS.md`, and `docs/agents-dev.md`.
- Inspected existing uniqueness tests for local and property aliases, constructor ownership transfer, nullable unique references, property assignment, consistency restoration, getters, and setters.
- Existing single-feature coverage already established constructor moves, nullable safe-call moves, sibling independence, and replacing a moved property. The focused gap was interaction coverage combining these behaviors in one heap-object matrix.

## Probes

Added `formver.compiler-plugin/testData/diagnostics/uniqueness_checker/heap_aliasing_mutation.kt` with these bounded cases:

| Feature pair or control | Expected and observed outcome |
| --- | --- |
| Constructor transfer + root consumption | Supported; the constructed object is consumable and both constructor arguments report `INVALID_MOVED_ACCESS` afterward. |
| Field move + sibling access | Supported; the independent sibling remains consumable. |
| Field move + root escape | Expected proof/ownership failure: `ESCAPE_UNIQUENESS_INCONSISTENCY` identifies `box.current`. |
| Field move + setter replacement | Supported; assigning a fresh object restores root consistency. |
| Nullable field move + root escape | Expected proof/ownership failure: `ESCAPE_UNIQUENESS_INCONSISTENCY` identifies `box.optional`. |
| Nullable field move + null replacement | Supported boundary; assigning `null` restores the moved slot and the root is consumable. |
| Constructor transfer + unique alias + mutation | Supported; mutation through the alias preserves a consumable root, while all transferred inputs report `INVALID_MOVED_ACCESS`. |

The generated golden contains exactly seven diagnostics: five expected invalid moved accesses and two expected escaping-value inconsistencies. No source diagnostic, unsupported conversion, internal error, backend failure, timeout, or semantic contradiction occurred.

## Commands and evidence

- `./agent-scripts/test.sh heap_aliasing_mutation` initially failed before tests because the environment selected Java 25.0.2, which this Gradle/Kotlin setup rejected.
- Ubuntu package installation was unavailable because the environment could not resolve the configured package mirrors. Downloaded a temporary Temurin 21.0.12.1 runtime under `/tmp` through GitHub; no repository file was affected.
- The first JDK 21 run generated the missing golden and failed with the expected harness message. Read the entire generated golden before accepting it.
- `JAVA_HOME=/tmp/snakt-jdk21 PATH=/tmp/snakt-jdk21/bin:$PATH ./agent-scripts/test.sh heap_aliasing_mutation` — 1 passed, 0 failed.
- `JAVA_HOME=/tmp/snakt-jdk21 PATH=/tmp/snakt-jdk21/bin:$PATH ./agent-scripts/test.sh --verify heap_aliasing_mutation` — 1 passed, 0 failed.
- `JAVA_HOME=/tmp/snakt-jdk21 PATH=/tmp/snakt-jdk21/bin:/tmp/snakt-z3/z3-4.8.7-x64-ubuntu-16.04/bin:$PATH ./agent-scripts/check-all.sh` — Gradle check passed and test-data checks passed. The wrapper exited 2 only because `pre-commit` was unavailable.
- Attempts to install or invoke `pre-commit` with both `pip` and `uvx` were blocked by the environment proxy returning 403 for `files.pythonhosted.org`.
- Ran the local hooks directly: `git diff --check`, `./agent-scripts/check-testdata.sh`, and `./agent-scripts/tests/run.sh` all passed. The remote `end-of-file-fixer` hook could not be installed through the blocked proxy; `git diff --check` provides the relevant whitespace validation for the changed files.

## Conclusion

The tested constructor, ownership, alias, mutable-field, sibling, and nullable-field interactions behave consistently with the established uniqueness model. No previously unreported bug was confirmed, so no bug issue was filed.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=4cb2cc75-f660-4c28-a0fd-ca9cc9b904af
