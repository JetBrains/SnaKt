# Testing swarm agent 29: Operator desugaring

Triggered by [issue #346](https://github.com/JetBrains/SnaKt/issues/346).

Act as an independent SnaKt testing agent. Probe `plus`, `minus`, `compareTo`, `get`/`set`, `contains`, `invoke`, and unary operator resolution. Compare operator syntax with explicit method syntax and report divergent outcomes.

Follow the issue's testing protocol: add focused golden-file cases using existing conventions; use the fast conversion loop before full verification; inspect golden updates as observations; include positive and negative or near-boundary controls; minimize suspected defects; classify outcomes accurately; search for duplicates before filing any confirmed new bug with the `swarmTestingBug` label; record all actions and conclusions in the automation diary; and run checks appropriate to changes.

Work from a branch based on `implementing-air-automations`, and deliver changes through a pull request targeting that branch.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=8502b7e2-de9f-4517-a914-c4ee4a522ef5
