# Testing swarm agent — issue 318 diary

## Actions

- Read `AUTOMATIONS.md` before performing repository investigation.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-318.md` as required.
- Confirmed the checkout started on `implementing-air-automations` at revision `9bac7b3`, inspected recent automation PR branches, and created `test/issue-318-integer-boundaries` from that source branch.
- Inspected existing operator, contract, golden-file, and conversion conventions before adding probes.
- Added paired `Int` contracts covering `MAX_VALUE` addition, `MIN_VALUE` negation, `MIN_VALUE / -1`, division controls, and remainder controls. Each exceptional boundary has a nearby or positive control.
- Added conversion probes for `Long.MAX_VALUE`, `Long.MIN_VALUE`, and `Long.toInt()` at and immediately beyond the `Int` maximum.
- Installed a temporary Temurin JDK 21 because the environment's JDK 25 was rejected by the Gradle/Kotlin setup before tests started.
- Installed the repository-documented Z3 4.8.7 binary after verification initially reached the backend without a prover.
- Ran conversion-only tests first, read the generated Viper output and all golden changes, then ran full verification.
- Compiled and ran an independent Kotlin 2.3.0 boundary probe. It produced: `Int.MAX_VALUE + 1 == Int.MIN_VALUE`, `-Int.MIN_VALUE == Int.MIN_VALUE`, `Int.MIN_VALUE / -1 == Int.MIN_VALUE`, `Int.MIN_VALUE % -1 == 0`, `Int.MIN_VALUE % Int.MAX_VALUE == -1`, `2147483647L.toInt() == 2147483647`, and `2147483648L.toInt() == Int.MIN_VALUE`.
- Minimized the negative-remainder discrepancy to `-2 % 3`, whose compiled Kotlin result is `-2` while SnaKt cannot prove the matching postcondition.
- Searched open and closed GitHub issues for integer overflow, negative remainder, modulo, modulus, and `remInts`. Overflow semantics are already tracked by issue 296; no issue covered negative-dividend remainder semantics.
- Opened [issue 382](https://github.com/JetBrains/SnaKt/issues/382), labeled `swarmTestingBug`, with the minimal reproducer, expected and observed results, implementation detail, environment, and revision.
- Ran `./agent-scripts/check-all.sh`: Gradle `check` and `check-testdata.sh` passed, but the command returned exit 2 because `pre-commit` was not installed. Attempts to install it with both `pip` in a temporary virtual environment and `uv` were blocked by the environment proxy returning HTTP 403 from `files.pythonhosted.org`; system package installation was unavailable to the unprivileged user.
- Ran every configured pre-commit hook directly as a fallback: `agent-scripts/tests/run.sh`, `agent-scripts/check-testdata.sh`, the `end-of-file-fixer` invariant over identified text files, and `git diff --check`; all passed.

## Conclusions

- `Int` boundary overflow is modeled as mathematical, unbounded integer arithmetic. Consequently, correct Kotlin/JVM wraparound postconditions fail for `MAX_VALUE + 1`, negating `MIN_VALUE`, and `MIN_VALUE / -1`; nearby non-overflow controls verify. This known semantics gap is covered by issue 296.
- `MIN_VALUE % -1 == 0` verifies, but negative remainders with non-divisors use Viper modulo semantics rather than Kotlin remainder semantics. Both the boundary case and minimized `-2 % 3` case fail proof; this newly reported defect is issue 382.
- `Long` literals are unsupported during conversion and emit `INTERNAL_ERROR`, so `Long` arithmetic and `Long.toInt()` narrowing do not reach proof. The paired narrowing probes distinguish this conversion failure from a verifier failure.
- Focused conversion and full-verification tests pass with their reviewed goldens.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=e2dc0e9c-45f6-46b6-8881-ef3bcf83d7df
