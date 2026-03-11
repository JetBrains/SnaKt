# Configuration

SnaKt is configured via the `formver { }` Gradle extension in your `build.gradle.kts`.

---

## Gradle DSL

```kotlin
formver {
    logLevel("only_warnings")                            // default
    errorStyle("user_friendly")                         // default
    conversionTargetsSelection("targets_with_contract") // default
    verificationTargetsSelection("targets_with_contract") // default
    unsupportedFeatureBehaviour("throw_exception")      // default
    dumpViperFiles(false)                               // default
}
```

---

## Options reference

### `logLevel`

Controls how much Viper output is emitted as compiler info diagnostics.

!!! tip
    Info messages are only visible when Gradle is run with `--info`.

| Value | Effect |
|-------|--------|
| `only_warnings` _(default)_ | No Viper output |
| `short_viper_dump` | Generated Viper program per function, without predicate definitions |
| `short_viper_dump_with_predicates` | Generated Viper program including predicate definitions |
| `full_viper_dump` | Complete Viper program including all declarations |

Equivalent to the `// RENDER_PREDICATES` file-level directive for a single file.

---

### `errorStyle`

Controls how Silicon verification failures are presented.

| Value | Effect |
|-------|--------|
| `user_friendly` _(default)_ | Translate to Kotlin-level diagnostics; fall back to raw Viper message |
| `original_viper` | Always show the raw Silicon/Viper error |
| `both` | Show both the translated message and the raw Viper error |

`both` is useful when debugging: you can see both the high-level message and the raw Silicon output that caused it.

---

### `conversionTargetsSelection` and `verificationTargetsSelection`

Control which functions enter each stage.

| Value | Functions in scope |
|-------|--------------------|
| `targets_with_contract` _(default)_ | Functions with a Kotlin `contract { }` block |
| `all_targets` | Every function in the module |
| `no_targets` | None (disables the stage) |

`conversionTargetsSelection` must be at least as broad as `verificationTargetsSelection` — you cannot verify a function that has not been converted.

Per-function annotations (`@AlwaysVerify`, `@NeverVerify`, `@NeverConvert`) always override these project-level settings.

---

### `unsupportedFeatureBehaviour`

Controls what happens when the plugin encounters a Kotlin construct it cannot translate.

| Value | Effect |
|-------|--------|
| `throw_exception` _(default)_ | Abort conversion; report `INTERNAL_ERROR` |
| `assume_unreachable` | Treat the unsupported construct as `assume false` and continue |

`assume_unreachable` is useful for isolating parts of a function when the unsupported feature is on a non-critical code path.

---

### `dumpViperFiles`

When `true`, the generated Viper program for each converted function is written to `.formver/<FunctionName>.vpr` inside the project directory, and its file URI is emitted as a compiler info diagnostic.

```kotlin
formver { dumpViperFiles(true) }
```

This is useful for inspecting the exact Viper program submitted to Silicon, especially when debugging verification failures or unsupported-feature errors.

!!! tip
    Run Gradle with `--info` to see the file URIs in the build output.

---

## Command-line options

When invoking the plugin directly via `kotlinc`, pass options with:

```bash
-P plugin:org.jetbrains.kotlin.formver:OPTION=VALUE
```

| Option | Gradle name | Example value |
|--------|-------------|---------------|
| `log_level` | `logLevel` | `full_viper_dump` |
| `error_style` | `errorStyle` | `both` |
| `conversion_targets_selection` | `conversionTargetsSelection` | `all_targets` |
| `verification_targets_selection` | `verificationTargetsSelection` | `no_targets` |
| `unsupported_feature_behaviour` | `unsupportedFeatureBehaviour` | `assume_unreachable` |
| `dump_viper_files` | `dumpViperFiles` | `true` |

---

## File-level directives

These comments at the top of a source file override project-level settings for that file:

| Directive | Effect |
|-----------|--------|
| `// UNIQUE_CHECK_ONLY` | Run only the uniqueness checker; skip Viper |
| `// ALWAYS_VALIDATE` | Force verification even if the project default is off |
| `// NEVER_VALIDATE` | Skip Viper verification for the whole file |
| `// RENDER_PREDICATES` | Include predicate definitions in Viper output |
| `// REPLACE_STDLIB_EXTENSIONS` | Use SnaKt's stdlib replacements (`run`, `let`, etc.) |
| `// WITH_STDLIB` | Include stdlib types and functions in the Viper model |