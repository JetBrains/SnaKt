# Testing Guide

SnaKt's test suite verifies the plugin's conversion and verification behaviour using the built-in Kotlin compiler test framework combined with JUnit 5.

---

## Test framework

Tests use the Kotlin compiler's built-in diagnostic test infrastructure together with JUnit 5. Each test:

1. Compiles a `.kt` test file through the Kotlin K2 compiler with the formver plugin active.
2. Collects the compiler diagnostics emitted by the plugin.
3. Compares the actual diagnostics against the expected output stored in a `.fir.diag.txt` file.

---

## Test data location

All test data lives under:

```
formver.compiler-plugin/testData/diagnostics/
```

Each subdirectory groups tests by category (see table below). Every test consists of a pair of files:

- `SomeTest.kt`: the Kotlin source to be compiled and verified.
- `SomeTest.fir.diag.txt`: the expected compiler diagnostic output.

---

## Test file format

### The .kt file

A test `.kt` file is a normal Kotlin file with one special comment at the top:

```kotlin
// FILE: SomeTest.kt
package test

import org.jetbrains.kotlin.formver.plugin.*

// DIAGNOSTICS: NONE

@AlwaysVerify
fun example(x: Int): Int {
    postconditions<Int> { result -> result == x + 1 }
    return x + 1
}
```

The `// DIAGNOSTICS: NONE` comment declares that this test expects no diagnostics. To declare that the test expects specific diagnostics, list their names:

```kotlin
// DIAGNOSTICS: PURITY_VIOLATION
```

Multiple diagnostic names can be listed space-separated. The comment tells the test framework which diagnostics are "expected" (not errors in the test output).

### The .fir.diag.txt file

The `.fir.diag.txt` file contains the full expected output from the compiler, including:

- Any diagnostic messages emitted by the plugin.
- The generated Viper program text (when `logLevel` is set above `ONLY_WARNINGS` in the test configuration).

This file is generated automatically the first time you run the test with `-Pupdate` (see below). Inspect it manually to verify correctness before committing.

---

## Test categories

| Directory | What it tests |
|---|---|
| `conversion/` | Correct FIR-to-Viper translation; checks generated Viper code structure |
| `purity_checker/` | `@Pure` annotation validation; expects `PURITY_VIOLATION` errors for impure bodies |
| `uniqueness_checker/` | Ownership/uniqueness analysis; expects `UNIQUENESS_VIOLATION` errors |
| `verification/contracts/` | Verification of Kotlin `contract { }` blocks and custom specs |
| `verification/expensive_diagnostics/` | Heavier verification tests that take longer to run |
| `verification/properties_and_fields/` | Property and field access verification |
| `verification/user_invariants/` | User-written `loopInvariants {}` and `preconditions {}`/`postconditions {}` |
| `verification/string/` | String operations (length, get, concatenation) |
| `verification/list/` | List operations (index access, sublist, iteration) |
| `verification/inlining/` | Functions that are inlined during conversion |
| `verification/uniqueness/` | Uniqueness/ownership properties in verification |

---

## Gradle commands

### Generate test runners (required once, and after adding new test files)

```bash
./gradlew generateTests
```

This reads all `.kt` files in `testData/diagnostics/` and generates a corresponding Java test runner class in `test-gen/`. The generated file should not be edited by hand.

### Run the full test suite

```bash
./gradlew :formver.compiler-plugin:test \
  --tests "org.jetbrains.kotlin.formver.plugin.runners.FirLightTreeFormVerPluginDiagnosticsTestGenerated"
```

### Run a single test

The test method name is derived from the `.kt` filename by removing the extension and capitalising the first letter:

```bash
./gradlew :formver.compiler-plugin:test \
  --tests "*FirLightTreeFormVerPluginDiagnosticsTestGenerated.testMyNewTest"
```

For a file named `my_new_test.kt`, the method name is `testMy_new_test` (underscores are preserved; only the first letter is capitalised).

### Update expected output

After an intentional change to the plugin's output, regenerate the expected `.fir.diag.txt` files:

```bash
./gradlew :formver.compiler-plugin:test -Pupdate
```

This runs all tests but instead of failing on mismatches, it overwrites the `.fir.diag.txt` files with the new actual output. Run without `-Pupdate` afterwards to confirm all tests pass.

---

## Adding a test

### Step 1: Create the .kt file

Place the file in the appropriate subdirectory. Choose `verification/contracts/` for contract-based specs or `conversion/` to check generated Viper code.

Minimal structure:

```kotlin
// FILE: MyNewTest.kt
package test

import org.jetbrains.kotlin.formver.plugin.*

// DIAGNOSTICS: NONE

@AlwaysVerify
fun example(x: Int): Int {
    postconditions<Int> { result -> result == x + 1 }
    return x + 1
}
```

### Step 2: Regenerate the test runners

```bash
./gradlew generateTests
```

### Step 3: Run the test with `-Pupdate` to generate the expected output

```bash
./gradlew :formver.compiler-plugin:test \
  --tests "*FirLightTreeFormVerPluginDiagnosticsTestGenerated.testMyNewTest" \
  -Pupdate
```

### Step 4: Inspect the generated .fir.diag.txt

Open the generated file and verify:

- No unexpected errors appear.
- The generated Viper code (if visible) looks correct.
- The Viper method has the pre/postconditions you expect.

### Step 5: Run without -Pupdate to confirm it passes

```bash
./gradlew :formver.compiler-plugin:test \
  --tests "*FirLightTreeFormVerPluginDiagnosticsTestGenerated.testMyNewTest"
```

### Step 6: Commit both files

Commit both `MyNewTest.kt` and `MyNewTest.fir.diag.txt` together. The `.fir.diag.txt` file is the ground truth for CI.

---

## The auto-generated test runner

The file `formver.compiler-plugin/test-gen/…/FirLightTreeFormVerPluginDiagnosticsTestGenerated.java` is generated by `./gradlew generateTests`. Do not edit it manually — your changes will be overwritten the next time `generateTests` is run.

If you need to change the test runner configuration, look at the generator source in the `test-fixtures/` module.

---

## CI

GitHub Actions runs the full test suite automatically on:

- Every push to `main`.
- Every pull request targeting `main`.

The CI configuration is in `.github/workflows/`. If CI fails on your PR:

1. Download the JUnit XML report from the Actions run.
2. Identify the failing tests.
3. Run the failing tests locally with `-Pupdate` if the output changed intentionally, or investigate the regression if it did not.
