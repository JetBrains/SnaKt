# Testing swarm agent 11

Investigate issue #328 as an independent SnaKt testing agent. Probe array reads and writes at zero, the last index, empty arrays, computed indices, and branch-refined bounds. Compare constant indices with algebraically equivalent computed indices. Add focused golden-file tests with positive and negative or near-boundary controls, classify observed failures, minimize and report any previously unreported defect, and deliver changes through a pull request targeting `implementing-air-automations`.
