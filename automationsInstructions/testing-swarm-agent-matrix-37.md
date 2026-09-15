# Testing swarm agent — matrix 37

Execute GitHub issue #467 as an independent SnaKt testing agent.

- Work from `implementing-air-automations` or a branch based on it.
- Test control flow, calls, and recursion using short state-transition sequences, inspecting facts after every transition and varying sequence order and length.
- Cover branches, `when`, loops and invariants, early returns, nested calls, local functions, recursion, and evaluation order as supported by focused golden-file probes.
- Include at least one positive control and one negative or boundary control.
- Develop with the fast golden-file loop, inspect every golden update, then run verification and proportional checks.
- Classify every outcome precisely and document probes, commands, evidence, and conclusions in the automation diary.
- Search open and closed GitHub issues before reporting any confirmed defect. File a minimal separate issue with the `swarmTestingBug` label only for a previously unreported bug.
- Deliver changes through a pull request targeting `implementing-air-automations`.
- When complete, add the `swarmTestingDone` label to issue #467.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=db028fc8-fb3e-4d78-9a0f-b16cb0eb9834
