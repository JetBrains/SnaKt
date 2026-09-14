# Testing swarm agent 41

Triggered by GitHub issue #358, "[Swarm test 41] Golden expectation integrity".

Act as an independent SnaKt testing agent on a branch based on
`implementing-air-automations`. Probe whether deliberately failing verification
cases produce their intended diagnostic instead of merely passing because a
failure was recorded in a golden. Add focused golden-file test data using
existing conventions, read every regenerated golden diff, and include positive
and negative or near-boundary controls. Develop with the conversion-only test
loop, then run verification once conversion behavior is understood. Minimize
and reproduce suspected defects, distinguish the failure stage, and search open
and closed issues before reporting a newly confirmed bug with the
`swarmTestingBug` label. Record all actions and conclusions in the automation
diary and run checks appropriate to changes before delivery.

Deliver changes through a signed commit on a new branch, push it, and open a
pull request targeting `implementing-air-automations` that references issue
#358.
