# Testing swarm: contract boundaries (#431)

Act as an independent SnaKt testing agent for issue #431. Exercise the Contract DSL and specification semantics using boundary-value analysis, including preconditions, postconditions, `verify`, quantifiers, implications, purity, contract propagation, accepted specifications, and invalid or impure forms. Add focused golden test data with positive and negative or boundary controls, classify outcomes precisely, search existing GitHub issues before reporting any confirmed defect, and keep a complete diary of probes, commands, evidence, and conclusions.

Work from a branch based on `implementing-air-automations`, develop with the fast golden test loop before full verification, inspect every golden update, run proportional checks, deliver changes in a pull request to `implementing-air-automations`, and add the `swarmTestingDone` label to issue #431 when finished. Any newly confirmed and previously unreported bug must be filed separately with the `swarmTestingBug` label.

Produced by Air Automations. Name: Testing swarm agent / Run: https://air.jetbrains.cloud/org/05cf1a7f-6ab5-713b-abd3-29d0c8a05e2d/automations/34ccbbdd-3fd2-474e-8f0a-10b4b2d5bda4?run=0fab2e56-cacf-496b-b810-81d12519ffcf
