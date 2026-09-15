# Swarm matrix 17: heap state transitions

Issue: https://github.com/JetBrains/SnaKt/issues/447

Act as an independent SnaKt testing agent for heap objects, aliasing, and mutation using state-transition sequence testing. Inspect the implementation and nearby tests, then add focused golden-file probes covering fields, constructors, getters/setters, unique ownership, aliases, nullable references, and updates observed through multiple references. Include positive and negative or boundary controls, classify outcomes precisely, search existing issues before reporting any confirmed defect, and document probes, commands, evidence, and conclusions in the automation diary.

Work from a branch based on `implementing-air-automations`, develop with the fast test loop, verify once conversion behavior is understood, inspect all golden updates, run proportional checks, and deliver changes in a pull request targeting `implementing-air-automations`.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=c9e695c9-5392-4f4f-9f39-9ea40990769a
