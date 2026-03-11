# Troubleshooting

This page covers common problems you may encounter when using SnaKt, and how to resolve them.

---

## Z3 not found

**Symptom:** Compilation fails with a message such as:

```
Silicon failed to start: Z3 executable not found
```

or Silicon crashes immediately at startup.

**Cause:** The `Z3_EXE` environment variable is not set, is set to the wrong path, or the shell that runs `kotlinc` / Gradle does not inherit it.

**Solution:**

1. Confirm Z3 is installed and executable:

    ```bash
    $Z3_EXE --version
    # Expected: Z3 version 4.8.7 - 64 bit
    ```

2. If the variable is not set in the current shell, set it:

    ```bash
    export Z3_EXE=/usr/local/bin/z3
    ```

3. To make the setting persistent, add the export to the appropriate shell startup file:

    | Shell / context | File |
    |-----------------|------|
    | `bash` interactive | `~/.bashrc` or `~/.bash_profile` |
    | `zsh` interactive | `~/.zshrc` |
    | Login shells / GUI apps (Linux) | `~/.profile` or `~/.xprofile` |
    | macOS GUI apps (launched from Finder/Dock) | `~/.zshrc` (for zsh-based terminals) or a launchd plist |

    After editing, reload the file (`source ~/.zshrc`) and open a new terminal window to verify the setting persists.

4. If you launch your IDE from a desktop shortcut rather than a terminal, the IDE process may not inherit your shell environment. Set `Z3_EXE` in the IDE's own JVM run environment or in `~/.profile` / `~/.xprofile`.

---

## Wrong Z3 version

**Symptom:** Silicon crashes with an error such as `Unexpected Z3 API version` or verification produces nonsensical results.

**Cause:** SnaKt requires **exactly Z3 v4.8.7**. Other versions — including newer ones — are not supported.

**Solution:**

1. Download the correct version from [https://github.com/Z3Prover/z3/releases/tag/z3-4.8.7](https://github.com/Z3Prover/z3/releases/tag/z3-4.8.7).
2. Replace or shadow the existing binary at the path pointed to by `Z3_EXE`.
3. Verify: `$Z3_EXE --version` must print `Z3 version 4.8.7`.

---

## StackOverflowError during compilation

**Symptom:** The Kotlin daemon crashes with a `StackOverflowError` during compilation or test execution:

```
java.lang.StackOverflowError
    at org.jetbrains.kotlin.fir.declarations...
```

**Cause:** The FIR traversal during plugin execution uses deep recursion. The default JVM stack size is insufficient for large functions or deep expression trees.

**Solution:** Increase the Kotlin daemon's stack size in your `build.gradle.kts`:

```kotlin
kotlin {
    kotlinDaemonJvmArgs = listOf("-Xss30m")
}
```

30 MB is the recommended value. Restart the Gradle daemon after making this change:

```bash
./gradlew --stop
./gradlew compileKotlin
```

---

## Verification timeout

**Symptom:** Compilation hangs or takes very long. Silicon eventually reports a timeout or fails with a resource exhaustion message.

**Cause:** The SMT solver takes exponential time on some quantified formulas. This often happens with complex `forAll` expressions where no useful trigger can be inferred automatically.

**Solutions:**

1. **Provide explicit triggers.** Use `triggers(expr)` inside the `forAll` block to guide the SMT solver:

    ```kotlin
    forAll<Int> {
        triggers(it * it)   // tell the solver when to instantiate
        it * it >= 0
    }
    ```

    See [SMT triggers](specifications.md#smt-triggers) for details.

2. **Simplify the specification.** Break a complex postcondition into smaller lemma functions, each with its own specification.

3. **Check for missing invariants.** If a loop body is slow to verify, the invariant may not be strong enough to let the solver discharge the proof obligation quickly. Add intermediate `verify()` calls to identify the bottleneck.

---

## INTERNAL_ERROR for unsupported features

**Symptom:** Compilation reports an error like:

```
error: An internal error has occurred. Details: <description of unsupported construct>
```

**Cause:** The plugin encountered a Kotlin construct it cannot yet translate. With the default `unsupportedFeatureBehaviour("throw_exception")` setting, this aborts conversion of the affected function.

**Solutions:**

1. **Work around the unsupported feature.** Refactor the function to avoid the unsupported construct. See [Supported Features](supported-features.md) for what is and is not supported.

2. **Use `assume_unreachable` for non-critical paths.** If the unsupported code is on a code path that does not affect the property you are verifying, switch to:

    ```kotlin
    formver {
        unsupportedFeatureBehaviour("assume_unreachable")
    }
    ```

    This treats the unsupported construct as `assume false`, which makes the path vacuously true. Use this with care: it can hide real verification issues if the assumed-unreachable path is actually reachable.

3. **Mark the function `@NeverConvert`.** If verification of the whole function is not needed, exclude it from conversion:

    ```kotlin
    @NeverConvert
    fun functionWithUnsupportedFeature() {
        // ...
    }
    ```

---

## Reading Viper errors

**Symptom:** The error message is cryptic, referencing Viper variable names or internal Viper constructs rather than your Kotlin source.

**Solution:** Set `errorStyle("both")` to see both the translated user-friendly message and the raw Silicon output:

```kotlin
formver {
    errorStyle("both")
}
```

This is the most useful setting for debugging: the user-friendly message tells you which Kotlin line failed, and the raw Silicon message tells you exactly which Viper assertion the solver could not prove.

If you only want the raw Viper output (useful when comparing against manually written Viper code):

```kotlin
formver {
    errorStyle("original_viper")
}
```

---

## Seeing the generated Viper program

**Symptom:** You want to inspect the Viper code the plugin generates for a function to understand why verification succeeds or fails.

**Solution:**

1. Enable Viper output in `build.gradle.kts`:

    ```kotlin
    formver {
        logLevel("full_viper_dump")
    }
    ```

2. Run Gradle with `--info` to make info diagnostics visible:

    ```bash
    ./gradlew compileKotlin --info 2>&1 | grep -A 50 "Generated Viper"
    ```

    `logLevel` values:

    | Value | Output |
    |-------|--------|
    | `only_warnings` | Nothing (default) |
    | `short_viper_dump` | Viper method body only |
    | `short_viper_dump_with_predicates` | Method body plus predicate definitions |
    | `full_viper_dump` | Complete Viper program |

3. Alternatively, add `// RENDER_PREDICATES` at the top of a single source file to enable `short_viper_dump_with_predicates` for that file only.

---

## Inspecting the intermediate IR with `@DumpExpEmbeddings`

**Symptom:** You suspect a bug in the plugin's conversion step and want to inspect the internal intermediate representation (ExpEmbedding) before it is linearized.

**Solution:** Annotate the function with `@DumpExpEmbeddings`:

```kotlin
@AlwaysVerify
@DumpExpEmbeddings
fun myFunction(x: Int): Int {
    postconditions<Int> { it == x + 1 }
    return x + 1
}
```

Run with `./gradlew compileKotlin --info`. The compiler will emit an `EXP_EMBEDDING` info diagnostic containing the full embedding tree for the function. This is primarily a tool for plugin developers.

---

## OutOfMemoryError: Metaspace after repeated builds

**Symptom:** After running `compileKotlin` several times in the same session, the build fails with:

```
e: java.lang.OutOfMemoryError: Metaspace
```

and Gradle reports:

```
Not enough memory to run compilation. Try to increase it via 'gradle.properties':
  kotlin.daemon.jvmargs=-Xmx<size>
```

**Cause:** The Kotlin compiler daemon is a long-lived JVM process that accumulates loaded class metadata (Metaspace) across repeated compilations. Each run of the plugin loads additional classes; eventually the daemon exhausts the space allocated for class metadata.

**Solutions:**

1. **Kill the daemon.** The quickest fix is to stop the Gradle daemon so that the next build starts a fresh JVM:

    ```bash
    ./gradlew --stop
    ./gradlew compileKotlin
    ```

2. **Increase the Metaspace limit.** To reduce how often this occurs, raise the maximum Metaspace size in your project's `gradle.properties`:

    ```properties
    kotlin.daemon.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
    ```

    Restart the daemon after editing `gradle.properties`:

    ```bash
    ./gradlew --stop
    ./gradlew compileKotlin
    ```

---

## CI: setting `Z3_EXE` in GitHub Actions

When running the SnaKt test suite (or a project that depends on SnaKt) in GitHub Actions, Z3 must be installed and `Z3_EXE` must be set as an environment variable.

The recommended approach uses the `cda-tum/setup-z3` action:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Z3
        uses: cda-tum/setup-z3@v1
        with:
          version: 4.8.7
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Set Z3_EXE
        run: echo "Z3_EXE=$(which z3)" >> $GITHUB_ENV

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'corretto'

      - name: Build and test
        run: ./gradlew :formver.compiler-plugin:test
```

The `Z3_EXE` environment variable must be set **before** any step that invokes the Kotlin compiler with the plugin loaded. Setting it via `$GITHUB_ENV` makes it available to all subsequent steps in the job.
