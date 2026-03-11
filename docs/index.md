# SnaKt: Kotlin Formal Verification

SnaKt is a **K2 compiler plugin for Kotlin** that performs formal verification of annotated code. It translates Kotlin functions into [Viper](https://www.pm.inf.ethz.ch/research/viper.html), a verification intermediate language, and checks them automatically using the [Silicon](https://github.com/viperproject/silicon) verifier backed by the [Z3](https://github.com/Z3Prover/z3) SMT solver.

You annotate your Kotlin functions with *preconditions*, *postconditions*, and *loop invariants*. The plugin proves mathematically — for all possible inputs — that your code satisfies its specification.

---

## Quick example

```kotlin
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun abs(x: Int): Int {
    postconditions<Int> { result ->
        result >= 0
        result == x || result == -x
    }
    return if (x >= 0) x else -x
}
```

Viper proves that `abs` always returns a non-negative value equal in magnitude to `x`. If you change the implementation to always return `x`, the plugin reports a verification error pointing to the violated postcondition.

---

## Key features

- **Preconditions and postconditions** — constrain function inputs and outputs
- **Loop invariants** — reason about loops with arbitrarily many iterations
- **Universal quantifiers** — state properties over all values of a type
- **Ownership checking** — track unique ownership with `@Unique` and `@Borrowed`
- **Kotlin contracts integration** — re-uses Kotlin's standard `contract { }` DSL
- **Gradle integration** — runs transparently as part of `kotlinc` compilation

---

## Project status

SnaKt is in **early development**. Large parts of Kotlin syntax are not yet supported. See [Supported Features](user-guide/supported-features.md) for a current list.

The plugin is not published to Maven Central; see [Installation](getting-started/installation.md) to build it locally.

Contact: [komi.golov@jetbrains.com](mailto:komi.golov@jetbrains.com) for questions, contributions, and thesis supervision.

---

## Where to go next

| Goal | Page |
|------|------|
| Set up the plugin | [Installation](getting-started/installation.md) |
| Write your first specification | [Quick Start](getting-started/quick-start.md) |
| Learn all annotations and built-ins | [Annotations & Built-ins](user-guide/annotations.md) |
| Understand the architecture | [Architecture Overview](architecture/overview.md) |
| Contribute to the plugin | [Developer Onboarding](developer-guide/onboarding.md) |
