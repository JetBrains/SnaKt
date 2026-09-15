# Swarm matrix 22 assignment

Triggered by [issue #452](https://github.com/JetBrains/SnaKt/issues/452).

Act as an independent SnaKt testing agent for collections, arrays, and indexing using semantics-preserving metamorphic rewrites. Exercise lists, supported mutable collections, object and primitive arrays, size facts, indexing reads and writes, empty and singleton cases, and bounds obligations. Add focused golden-file probes following existing conventions, including at least one positive control and one negative or boundary control. Compare conversion, diagnostics, and verification outcomes across equivalent rewrites; classify each outcome precisely; minimize surprising cases; search existing GitHub issues before reporting a confirmed defect; and document probes, commands, evidence, and conclusions in the automation diary. If a previously unreported bug is confirmed, open a separate issue with a minimal reproducer and apply `swarmTestingBug`, never `bugFound`. Work from a branch based on `implementing-air-automations`, deliver changes through a pull request targeting that branch, and add `swarmTestingDone` to issue #452 when complete.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=e50f9b43-708b-4dff-a20c-795cb5e8337f
