# Quick Start

This guide walks you through writing and verifying your first Kotlin specification from scratch.

---

## 1. Import the annotations

All SnaKt annotations and built-in functions live in `org.jetbrains.kotlin.formver.plugin`:

```kotlin
import org.jetbrains.kotlin.formver.plugin.*
```

---

## 2. Write a function with a postcondition

```kotlin
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun max(a: Int, b: Int): Int {
    postconditions<Int> { result ->
        result >= a
        result >= b
        result == a || result == b
    }
    return if (a >= b) a else b
}
```

`@AlwaysVerify` tells the plugin to verify this function regardless of project-level settings. `postconditions<Int> { result -> ... }` declares three properties the function must guarantee. Each statement in the block is a separate predicate that must hold.

---

## 3. Run verification

```bash
./gradlew compileKotlin --info 2>&1 | grep -E "ERROR|Viper"
```

A successful verification produces no output. With `logLevel("full_viper_dump")` in your `formver { }` block, you can also see the generated Viper program.

---

## 4. Introduce a deliberate error

Change the return statement to always return `a`:

```kotlin
return a  // wrong: violates result == b when b > a
```

Run again. You should see a verification error pointing to the line `result == a || result == b`.

---

## 5. Add a precondition

```kotlin
@AlwaysVerify
fun divide(a: Int, b: Int): Int {
    preconditions {
        b != 0
    }
    postconditions<Int> { result ->
        result * b == a
    }
    return a / b
}
```

`preconditions { ... }` declares what callers must guarantee. The verifier assumes these hold inside the function and checks them at every call site.

---

## 6. Add a loop invariant

```kotlin
@AlwaysVerify
fun sumUpTo(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { result -> result == n * (n + 1) / 2 }

    var sum = 0
    var i = 0
    while (i < n) {
        loopInvariants {
            i <= n
            sum == i * (i + 1) / 2
        }
        sum += i + 1
        i++
    }
    return sum
}
```

`loopInvariants { ... }` goes at the top of the loop body. It establishes what is true before the loop starts and must be re-proved at the end of each iteration.

---

## Next steps

- [Annotations & Built-ins](../user-guide/annotations.md) — full reference for all annotations and specification functions
- [Writing Specifications](../user-guide/specifications.md) — detailed examples including quantifiers and contracts
- [Configuration](configuration.md) — Gradle DSL options
