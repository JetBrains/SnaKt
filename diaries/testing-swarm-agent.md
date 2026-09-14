# Testing swarm agent diary

- Read `AUTOMATIONS.md` before taking any task action.
- Read the full issue #337 trigger payload and delivery instructions.
- Confirmed the checkout starts clean on `implementing-air-automations`.
- Inspected recent PR branch names to infer the `test/issue-<number>-<topic>` convention.
- Recorded this assignment in `automationsInstructions/testing-swarm-agent.md`.
- Used semantic code search to locate existing mutable-field and uniqueness tests.
- Read the closest golden sources (`unique_fields.kt`, `manual_permissions.kt`,
  `call.kt`, `consistency.kt`, and branch assignment cases) plus the test-driver
  documentation.
- Identified a coverage gap for minimal sibling mutable fields observed across
  helper calls and branch joins.
- Added `mutable_field_permissions.kt` with shared and unique read/move/write
  probes, restoration controls, and asymmetric branch controls.
- Installed a temporary Temurin JDK 21 after the host JDK 25 failed during
  Gradle configuration; the unprivileged package manager and direct vendor
  downloads were unavailable, so fetched the release asset through GitHub.
- Ran the focused conversion test. Its first successful compilation generated
  the required diagnostic golden; rerunning passed 1/1.
- Ran `--update-goldens` and read the complete report. The three recorded
  diagnostics matched the intended negative controls: shared-to-unique call,
  unrestored branch escape, and shared-to-unique assignment.
- Added a direct shared-root field replacement control and an explicit read of
  the moved field alongside a valid read of its sibling.
- Reran the fast loop and inspected its expected/actual diff; the only new
  diagnostic was the deliberately marked invalid read of the moved field.
- Regenerated the golden and read the complete report. It records exactly four
  intended diagnostics and confirms the shared-root replacement is accepted.
- Ran the full focused pipeline with `--verify`; 1/1 test passed.
- Concluded that the probes show field-local permission tracking across calls
  and branches, with no confirmed defect to report.
- Ran `check-all.sh`. The first run exposed missing JDK 21, Z3 4.8.7, and
  pre-commit tooling rather than repository failures.
- Bootstrapped the pinned tools locally. Because the package proxy rejected
  Python package downloads, used the official pre-commit zipapp and an offline
  wheel cache for its end-of-file hook environment.
- Reran `check-all.sh` successfully: Gradle check, testData checks, end-of-file
  fixer, and script tests all passed.
