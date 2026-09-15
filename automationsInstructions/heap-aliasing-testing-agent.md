# Heap aliasing testing agent

Triggered by [issue #446](https://github.com/JetBrains/SnaKt/issues/446).

Act as an independent SnaKt testing agent for heap objects, aliasing, and mutation. Use pairwise feature interaction testing across fields, constructors, getters and setters, unique ownership, aliases, nullable references, and updates observed through multiple references. Add focused golden-file probes with positive and negative or boundary controls; classify outcomes precisely; search existing issues before reporting a confirmed defect; and document commands, evidence, and conclusions in the automation diary. Any new defect must be reported separately with the `swarmTestingBug` label.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=4cb2cc75-f660-4c28-a0fd-ca9cc9b904af
