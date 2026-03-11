# Contributing

Thank you for your interest in contributing to SnaKt! This page explains how to contribute code, what to expect from the review process, and how the project relates to research.

---

## Contribution workflow

1. **Open an issue** (or pick an existing one) on GitHub describing the bug you want to fix or the feature you want to add. This allows maintainers to give early feedback before you invest time in implementation.

2. **Create a branch** from `main`:
   ```bash
   git checkout main
   git pull
   git checkout -b my-feature-branch
   ```

3. **Make your changes** following the patterns established in the codebase. See [Adding Features](adding-features.md) for common recipes.

4. **Add or update tests** — every change to conversion or verification behaviour must be covered by a test. See [Testing Guide](testing.md) for the full procedure.

5. **Run the full test suite** locally before pushing:
   ```bash
   ./gradlew :formver.compiler-plugin:test \
     --tests "org.jetbrains.kotlin.formver.plugin.runners.FirLightTreeFormVerPluginDiagnosticsTestGenerated"
   ```

6. **Open a pull request** targeting `main`. Describe what the change does and why. Link to the relevant issue.

7. **CI** runs automatically on every PR. If it fails, check the JUnit report linked in the Actions run, fix the issue, and push again.

8. **Request review** from a maintainer (see Contact below). Expect at least one round of review comments.

---

## Code style

The project follows the [official Kotlin code style](https://kotlinlang.org/docs/coding-conventions.html). This is declared in `gradle.properties`:

```
kotlin.code.style=official
```

IntelliJ IDEA and Android Studio pick this up automatically. To apply the code style from the command line:

```bash
./gradlew ktlintFormat
```

Key conventions used throughout the codebase:

- **Sealed interfaces** over sealed classes wherever possible.
- **Data classes** for immutable context objects (e.g. `StmtConverter`, `Linearizer`, `PureLinearizer`).
- **`copy` with updated fields** to model state transitions without mutation.
- **Context receivers** (`context(nameResolver: NameResolver)`) for functions that need a pervasive dependency.
- **KDoc comments** on all public and internal APIs explaining the *why*, not just the *what*.

---

## Contact

**Primary maintainer**: Komi Golov — komi.golov@jetbrains.com

Please do not send implementation questions by email without first opening a GitHub issue. Issues are tracked and visible to the whole team; emails can easily be lost.

---

## Thesis and research supervision

SnaKt is a research tool developed at JetBrains Research. The project is open to bachelor's and master's thesis collaborations that extend the range of supported Kotlin features or improve the verification methodology.

If you are interested in thesis supervision, contact Komi Golov directly with a brief description of what you would like to work on.

Contributions that extend supported Kotlin features (new expression types, new stdlib mappings, new annotation semantics) are particularly welcome and are a good match for thesis-scale projects.

---

## Silicon publishing

If your contribution requires updating the Silicon dependency (the Viper verifier library), additional steps are needed.

Silicon is published to the JetBrains Space Maven repository. The procedure is documented in `dev-info.md` at the repository root and involves:

1. Applying the SnaKt-specific patch to the Silicon source tree.
2. Building Silicon with `sbt`.
3. Publishing to the Space Maven repository using Space credentials stored in a local credentials file.
4. Updating the dependency version in SnaKt's `build.gradle.kts`.

Do not attempt to update Silicon without reading `dev-info.md` first. Coordinate with a maintainer if you are unsure whether a Silicon update is needed.

---

## What contributions are welcome

- **New expression support**: adding conversion and linearization for Kotlin expressions not yet handled.
- **New stdlib special functions**: mapping Kotlin stdlib operations to Viper built-ins.
- **Error message improvements**: making verification error messages more precise or actionable.
- **Test coverage**: adding test cases for edge cases and previously untested paths.
- **Documentation improvements**: fixing inaccuracies, adding examples, improving clarity.
- **Performance**: reducing verification time for large functions (e.g. by improving the Viper encoding).

### What is out of scope for external contributions

- Changes to the Silicon/Viper core libraries (these are separate projects with their own contribution processes).
- Changes to the K2 compiler infrastructure (these go through the Kotlin compiler project).
