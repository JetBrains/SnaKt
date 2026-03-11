# Developer Onboarding

Welcome to SnaKt! This guide walks you from zero to making your first code change.

---

## Background knowledge

### Required

- **Kotlin**: the plugin is written entirely in Kotlin. Familiarity with data classes, sealed interfaces, lambdas, extension functions, and context receivers is assumed.
- **Hoare logic**: the basis for preconditions, postconditions, and loop invariants. The [Viper tutorial introduction](http://viper.ethz.ch/tutorial/) provides a concise overview.

### Helpful but not required to start

- **Kotlin compiler internals (K2/FIR)**: the plugin hooks into the K2 compiler's Frontend Intermediate Representation layer. You do not need to understand this deeply to add tests or fix small bugs, but it is important when modifying the conversion engine.
- **Viper**: the verification intermediate language the plugin targets. The [Viper tutorial](http://viper.ethz.ch/tutorial/) is the best starting point.
- **Separation logic and permissions**: Viper uses separation logic to reason about heap access. The key idea is that to read or write a heap field you must hold a permission to it.
- **SMT solvers (Z3)**: a surface-level understanding (what an SMT solver does, what "unsat" means) is enough for most contributions.

---

## Prerequisites

See the [Installation page](../getting-started/installation.md) for full instructions. In summary:

- **JDK 21**: required by the Kotlin compiler and Silicon.
- **Z3 version 4.8.7**: the exact version is required; set `Z3_EXE` to point to the binary.
- **Git**: any recent version.

Gradle is included via the Gradle wrapper (`./gradlew`); no separate installation is needed.

---

## Clone, build, publish

```bash
git clone https://github.com/jesyspa/SnaKt.git
cd SnaKt
./gradlew build
```

On first run Gradle downloads all dependencies including Silicon from the JetBrains Space Maven repository. This may take several minutes.

To publish the built artifacts to your local Maven repository (needed to use the plugin in another project):

```bash
./gradlew publishToMavenLocal
```

---

## Run the tests

### Generate test runners (do this once, or after adding test data)

```bash
./gradlew generateTests
```

This regenerates the auto-generated Java test runner files from the test data in `formver.compiler-plugin/testData/`.

### Run the full test suite

```bash
./gradlew :formver.compiler-plugin:test \
  --tests "org.jetbrains.kotlin.formver.plugin.runners.FirLightTreeFormVerPluginDiagnosticsTestGenerated"
```

This takes 5–10 minutes on first run. Subsequent runs are faster due to Gradle's incremental build.

### Run a single test

Each test corresponds to a `.kt` file in `testData/diagnostics/`. To run the test for `testData/diagnostics/verification/contracts/SomeTest.kt`:

```bash
./gradlew :formver.compiler-plugin:test \
  --tests "*FirLightTreeFormVerPluginDiagnosticsTestGenerated.testSomeTest"
```

The test method name is derived from the file name by stripping the extension and capitalising the first letter.

### Update expected output

If you intentionally changed the Viper output or diagnostics, update the expected `.fir.diag.txt` files:

```bash
./gradlew :formver.compiler-plugin:test -Pupdate
```

### Interpreting test output

Each test compares the actual compiler diagnostics against the expected output in the `.fir.diag.txt` file. A mismatch means either the output changed unexpectedly (a regression) or the expected file needs updating (after an intentional change).

---

## Repository layout

```
formver.annotations/          ← user-facing annotations (@AlwaysVerify, @Pure, @Unique, …)
                                and built-in spec functions (preconditions {}, forAll, …)
formver.common/               ← shared config types (PluginConfiguration, LogLevel, ErrorStyle)
formver.compiler-plugin/
    cli/                      ← command-line option parsing
    core/                     ← FIR-to-Viper conversion engine (embeddings, linearization)
    plugin/                   ← K2 FirDeclarationChecker integration and error reporting
    uniqueness/               ← ownership/uniqueness analysis
    viper/                    ← Viper AST definitions and Silicon bridge
    testData/                 ← test .kt files + expected .fir.diag.txt output
    test-fixtures/            ← shared test infrastructure
    test-gen/                 ← auto-generated test runners (do not edit)
formver.gradle-plugin/        ← Gradle plugin
```

---

## Codebase tour

Work through these eight steps in order. Each step builds on the previous one.

### Step 1: User API

Read the user-facing API to understand what plugin users can do:

- `formver.annotations/…/Annotations.kt`: annotation definitions (`@AlwaysVerify`, `@Pure`, `@Unique`, `@NeverConvert`, `@NeverVerify`, `@DumpExpEmbeddings`, …).
- `formver.annotations/…/Builtins.kt`: built-in spec functions (`preconditions {}`, `postconditions {}`, `loopInvariants {}`, `forAll`, `verify`, …).

Then read the [Quick Start guide](../getting-started/quick-start.md) to see how these are used together.

### Step 2: Configuration

Read `formver.common/…/PluginConfiguration.kt` to understand the configuration knobs: `logLevel`, `errorStyle`, `conversionSelection`, `verificationSelection`, `unsupportedFeatureBehaviour`.

### Step 3: Plugin entry point

Read:

```
formver.compiler-plugin/plugin/src/…/plugin/compiler/ViperPoweredDeclarationChecker.kt
```

This is the `FirSimpleFunctionChecker` the K2 compiler calls for each function. It:
1. Gates on the conversion/verification selection policy.
2. Calls `ProgramConverter.registerForVerification` to convert the function.
3. Optionally logs the Viper text or dumps expression embeddings.
4. Calls `Verifier.checkConsistency` then `Verifier.verify`.
5. Translates errors via `reportVerifierError`.

### Step 4: Conversion engine

Read the three conversion contexts in order:

1. `core/conversion/ProgramConverter.kt` — program-level context; owns all registries.
2. `core/conversion/MethodConverter.kt` — function-level context; resolves parameters.
3. `core/conversion/StmtConverter.kt` — statement-level context; immutable data class.

See [Conversion Engine](../implementation/conversion.md) for a full description.

### Step 5: Embedding types

The intermediate representation is built from sealed-interface hierarchies:

- `core/embeddings/expression/ExpEmbedding.kt` — sealed interface for all expression nodes.
- `core/embeddings/types/TypeEmbedding.kt` — type representation.

Spend time understanding the sealed hierarchies; these are the most important data structures in the codebase.

### Step 6: Linearization

After conversion, linearization flattens the `ExpEmbedding` tree to Viper statements:

- `core/linearization/Linearizer.kt` — main linearization context.
- `core/linearization/SsaConverter.kt` — SSA variable versioning for pure functions.

See [Linearization](../implementation/linearization.md) for a full description.

### Step 7: Viper AST

The linearizer produces nodes from the Viper AST module:

- `viper/ast/Program.kt` — top-level structure.
- `viper/ast/Method.kt` — methods with pre/postconditions.
- `viper/ast/Stmt.kt` — sealed statement hierarchy.
- `viper/ast/Exp.kt` — sealed expression hierarchy.

See [Viper AST](../implementation/viper-ast.md) for a full description.

### Step 8: Silicon interface

Read how the Viper AST is submitted to Silicon:

```
formver.compiler-plugin/viper/src/…/viper/Verifier.kt
```

This converts the Kotlin-side Viper AST to Silver (via `toSilver()`) and calls the Silicon library's `DefaultMainVerifier`.

---

## Write your first spec

See the [Quick Start guide](../getting-started/quick-start.md) for a step-by-step walkthrough of writing and verifying a Kotlin function spec using the plugin.

---

## Debugging tips

### See the generated Viper code

Set `logLevel("full_viper_dump")` in your `formver { }` block and run Gradle with `--info`. Search the output for `Viper` to find the generated program.

### Dump expression embeddings

Annotate a function with `@DumpExpEmbeddings` to emit the intermediate `ExpEmbedding` tree as a diagnostic. This is useful when the Viper output looks wrong and you want to inspect the intermediate stage.

### Show both error representations

```kotlin
formver { errorStyle("both") }
```

This shows both the user-friendly diagnostic and the raw Silicon error text, which helps understand what Silicon actually failed to verify.

### Use assume_unreachable to continue past unsupported features

```kotlin
formver { unsupportedFeatureBehaviour("assume_unreachable") }
```

When an unsupported feature is encountered, the plugin inserts `assume false` (making Viper assume the code is unreachable) instead of throwing an exception. This lets verification continue past unsupported parts.

### Increase JVM stack size

The Kotlin daemon needs a large stack for deep recursion through the FIR AST:

```kotlin
kotlinDaemonJvmArgs = listOf("-Xss30m")
```

If you see `StackOverflowError`, increase this value.

### Check Z3 is correctly installed

```bash
echo $Z3_EXE
$Z3_EXE --version
# Expected: Z3 version 4.8.7 - 64 bit
```

If `Z3_EXE` is not set or the binary is not found, tests will hang or fail with unhelpful errors.
