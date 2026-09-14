# Conflict solving

Monitor automation pull requests and resolve merge conflicts when they appear.
For pull request #399, read `AUTOMATIONS.md`, check the source branch against
its `implementing-air-automations` base, and make only the changes needed to
restore mergeability.

For run `33b8418a-898e-47d0-9caf-40e0823e1886`, re-check pull request
#399 after its synchronize event and resolve conflicts only if the source no
longer merges cleanly into `implementing-air-automations`.

For run `cfc769e2-c997-400a-87b2-6cfdfb19cbaf`, inspect pull request #399
after its latest synchronize event, verify its source branch still merges into
`implementing-air-automations`, and resolve only conflicts that actually exist.

For run `e079ec54-1f25-4c0f-89dc-529068bcf6a4`, inspect pull request #399
after synchronization to `aa891d4162a6c5db14849e53dbc709d158eac278`,
verify it against the current `implementing-air-automations` base, and resolve
only conflicts that actually exist.
