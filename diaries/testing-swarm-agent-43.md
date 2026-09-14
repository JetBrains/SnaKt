# Testing swarm agent 43 diary

- Read `AUTOMATIONS.md` before taking any other repository action.
- Recorded the assignment in `automationsInstructions/testing-swarm-agent-43.md`.
- Inspected recent pull requests targeting `implementing-air-automations` and
  followed their testing branch convention by creating
  `test/issue-360-statement-reordering` from that source branch.
- Used semantic search and a read-only context exploration to inspect existing
  local-variable, verification, diagnostic, and golden-file conventions.
- Chose three focused metamorphic pairs with disjoint local state: reordered
  declarations that verify, reordered assignments that verify, and reordered
  assignments with an intentionally false assertion as the negative control.
- Added `verification/statement_reordering.kt` with those six probes.
- The first fast-loop attempt could not start under the environment's Java
  25.0.2. Installed Temurin 21.0.12.1 temporarily and reran the focused test.
- The fast conversion loop then reached the new test and failed only because
  its golden did not exist. Regenerated and read the full conversion golden;
  it preserves each source ordering in the generated Viper statements.
- Full verification initially could not start because Z3 was absent. Installed
  the repository-required Z3 4.8.7 temporarily and reran verification.
- Read the complete verification-golden report. The four positive functions
  produce no warnings. Each intentionally false reordered variant produces the
  same single `Assert might fail` warning, for two identical warnings total.
- Reran `./agent-scripts/test.sh --verify statement_reordering` with JDK 21 and
  Z3 4.8.7: one test passed and none failed.
- Concluded that declaration and assignment reordering over the tested disjoint
  state preserves conversion, proof outcomes, and diagnostics. No defect was
  observed and no bug issue was warranted.
- Ran `./agent-scripts/check-all.sh` with JDK 21 and Z3 4.8.7. Gradle check and
  testData checks passed; the command returned exit 2 only because pre-commit
  was unavailable.
- Tried to install pre-commit with both pip and uv, but the environment proxy
  returned HTTP 403 for Python package downloads. Ran every configured hook
  directly instead: testData checks passed, all agent-script assertions passed,
  the end-of-file convention passed, and `git diff --check` passed.
