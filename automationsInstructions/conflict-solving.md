# Conflict solving

Monitor automation pull requests and resolve merge conflicts when they appear.
For pull request #399, read `AUTOMATIONS.md`, check the source branch against
its `implementing-air-automations` base, and make only the changes needed to
restore mergeability.

For run `33b8418a-898e-47d0-9caf-40e0823e1886`, re-check pull request
#399 after its synchronize event and resolve conflicts only if the source no
longer merges cleanly into `implementing-air-automations`.
