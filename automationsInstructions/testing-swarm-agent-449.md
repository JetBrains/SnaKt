# Testing swarm agent — issue 449

Triggered by [issue 449](https://github.com/JetBrains/SnaKt/issues/449), this run must independently test heap objects, aliasing, and mutation for determinism and repeatability. It must add focused golden-file probes with positive and negative or boundary controls, compare unchanged and harmlessly perturbed equivalents across generated Viper, diagnostics, proof result, exit status, and golden output, classify outcomes precisely, search existing issues before reporting any confirmed defect, and document commands, evidence, and conclusions in the automation diary. Any newly confirmed defect must be filed separately with the `swarmTestingBug` label. When finished, issue 449 must receive the `swarmTestingDone` label.

Work must be based on `implementing-air-automations`, delivered through a signed commit and pull request targeting that branch, and follow `AUTOMATIONS.md` plus the repository testing instructions.
