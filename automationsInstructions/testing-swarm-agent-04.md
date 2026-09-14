# Testing swarm agent 04

Issue: https://github.com/JetBrains/SnaKt/issues/321

Act as an independent SnaKt testing agent. Probe subject and subjectless `when`
expressions, exhaustiveness, guards, `else` branches, and nested returns. Vary
branch order and insert unreachable branches to detect incorrect path
assumptions. Add focused golden-file test data with positive and negative or
near-boundary controls, inspect all observed golden behavior, minimize and
classify suspected defects, and report confirmed non-duplicate bugs separately
with the `swarmTestingBug` label. Record actions and conclusions in the
automation diary and deliver changes through a PR to
`implementing-air-automations`.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=a02a4563-dd2f-4018-9c53-5a8fc3c3c230
