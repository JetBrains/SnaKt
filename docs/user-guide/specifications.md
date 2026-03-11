# Writing Specifications

This guide covers all aspects of writing formal specifications in SnaKt. SnaKt translates Kotlin code with specifications to [Viper](https://www.pm.inf.ethz.ch/research/viper.html) for verification. Familiarity with Hoare logic is helpful; see the [Viper tutorial](http://viper.ethz.ch/tutorial/) if needed.

---

## Verification control

By default, SnaKt only verifies functions with Kotlin `contract { }` blocks. To verify functions with SnaKt specifications, use `@AlwaysVerify`:

```kotlin
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun divide(numerator: Int, denominator: Int): Int {
    preconditions { denominator != 0 }
    return numerator / denominator
}
```

**Per-function annotations:**

- `@AlwaysVerify` — verify this function regardless of plugin settings
- `@NeverVerify` — skip verification even if contracts are present
- `@NeverConvert` — skip Viper conversion entirely

**Project-level configuration** in `build.gradle.kts`:

```kotlin
formver {
    verificationTargetsSelection("all_targets")  // or "targets_with_contract" (default), "no_targets"
}
```

`@AlwaysVerify` always overrides project settings.

---

## Preconditions and postconditions

```kotlin
@AlwaysVerify
fun abs(x: Int): Int {
    postconditions<Int> { result ->
        result >= 0
        result == x || result == -x
    }
    return if (x >= 0) x else -x
}
```

Multiple conditions in a block are implicitly conjoined: all must hold. The `postconditions` block receives the return value as its lambda parameter; `it` is available as shorthand.

### Precondition example

```kotlin
@AlwaysVerify
fun accessString(idx: Int) {
    preconditions {
        0 <= idx
        idx < 3
    }
    // The verifier now knows: 0 <= idx < 3
    verify(0 <= idx, idx < 3, idx != 100)
    verify("aaa"[idx] == 'a')
}
```

### Postcondition shorthand

```kotlin
@AlwaysVerify
fun returnGreater13(): Int {
    postconditions<Int> { it > 13 }
    return 16
}
```

### Combining pre- and postconditions

```kotlin
@AlwaysVerify
fun subtractTen(int: Int): Int {
    preconditions {
        int > 10
    }
    postconditions<Int> {
        it > 0
    }
    return int - 10
}
```

---

## Loop invariants

```kotlin
@AlwaysVerify
fun sumUpTo(n: Int): Int {
    preconditions { n >= 0 }
    var sum = 0
    var i = 0
    while (i <= n) {
        loopInvariants {
            i >= 0
            sum == i * (i - 1) / 2
        }
        sum += i
        i++
    }
    return sum
}
```

The semantics of loop invariants in Viper:

- The invariant must hold when the loop is first entered.
- The loop body may assume the invariant holds at the top of each iteration.
- The invariant must be re-established at the end of every iteration.
- The invariant must hold when the loop exits.
- Code after the loop may assume the invariant holds and the loop condition is false.

### Nested loops

Each `while` loop has its own `loopInvariants { }` block:

```kotlin
@AlwaysVerify
fun loopInsideLoop() {
    var i = 0
    while (i < 10) {
        loopInvariants {
            i <= 10
        }
        var j = i + 1
        while (j < 10) {
            loopInvariants {
                i < j
                j <= 10
            }
            j = j + 1
        }
        i = i + 1
    }
}
```

### Loops with `break`

Loop invariants still apply when using `break`. The invariant must hold at the point of the `break`:

```kotlin
@AlwaysVerify
fun withBreak() {
    var i = 0
    while (true) {
        loopInvariants {
            i <= 10
        }
        if (i >= 10) break
        i++
    }
    verify(i == 10)
}
```

### Complete example: iterative sum with proven correctness

```kotlin
@AlwaysVerify
fun sumOfIntegersUpToN(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { res -> res == n * (n + 1) / 2 }

    var sum = 0
    var i = 0
    while (i < n) {
        loopInvariants {
            i <= n
            sum == i * (i + 1) / 2
        }
        sum += i + 1
        ++i
    }
    return sum
}
```

---

## Universal quantification

Use `forAll<T>` for universally quantified formulas. It is used inside `preconditions`, `postconditions`, and `loopInvariants` blocks.

```kotlin
@AlwaysVerify
fun example(arr: IntArray): Unit {
    preconditions {
        forAll<Int> { j ->
            (0 <= j && j < arr.size()) implies (arr[j] > 0)
        }
    }
    // ...
}
```

The `implies` infix operator is provided for convenience (`a implies b` is equivalent to `!a || b`).

### `forAll` in postconditions

```kotlin
// Every integer squared is non-negative
fun anyIntegerSquaredAtLeastZero(): Int {
    postconditions<Int> { res ->
        forAll<Int> {
            it * it >= 0
            it * it >= res
        }
    }
    return 0
}

// For all non-zero integers, the square is at least 1
fun anyIntegerSquaredIsAtLeastOneExceptZero(): Int {
    postconditions<Int> { res ->
        forAll<Int> {
            (it != 0) implies (it * it >= res)
        }
    }
    return 1
}
```

### `forAll` in loop invariants

```kotlin
// Find first position in string where character is >= c
@AlwaysVerify
fun String.firstAtLeast(c: Char): Int {
    postconditions<Int> { res ->
        0 <= res && res <= length
        forAll<Int> {
            (0 <= it && it < res) implies (get(it) < c)
        }
        (res != length) implies (get(res) >= c)
    }

    var i = 0
    while (i < length) {
        loopInvariants {
            0 <= i && i <= length
            forAll<Int> {
                (0 <= it && it < i) implies (get(it) < c)
            }
        }
        if (get(i) >= c) break
        ++i
    }
    return i
}
```

---

## SMT triggers

By default, Viper infers triggers automatically. You can specify them explicitly with `triggers()` inside a `forAll` block:

```kotlin
forAll<Int> { x ->
    triggers(x * x)        // single trigger
    x * x >= 0
}

forAll<Int> { x ->
    triggers(x * x, x + 1) // multiple triggers (each is a separate trigger set)
    x != 0 implies (x * x > 0)
}
```

Each argument to `triggers()` becomes a separate trigger. This differs from Viper syntax where you can group multiple expressions into a single trigger; currently SnaKt only supports simple (single-expression) triggers.

Triggers can also appear inside `loopInvariants`:

```kotlin
fun withTriggersInLoop(str: String): Int {
    var res = 0
    var i = 10
    while (i > 0) {
        loopInvariants {
            forAll<Int> {
                triggers(str[it])
                (0 <= it && it < str.length) implies ((str[it] - 'a') * (str[it] - 'a') >= res)
            }
        }
        i--
    }
    return res
}
```

---

## The `verify()` function

`verify()` asserts that one or more boolean expressions hold at the current program point. It is useful for stating intermediate properties inside a function body.

```kotlin
@AlwaysVerify
fun demonstrateVerify(x: Int) {
    verify(x + 1 > x)
    verify(
        0 <= x || x < 0,
        true,
    )
}
```

Multiple arguments are each checked independently. Arguments must be pure expressions — incrementing a variable inside `verify()` produces a `PURITY_VIOLATION` error.

---

## Logical connectives

Inside specification blocks, use standard Kotlin boolean operators plus the `implies` infix function:

| Expression | Meaning |
|-----------|---------|
| `a && b` | Logical conjunction |
| `a \|\| b` | Logical disjunction |
| `!a` | Logical negation |
| `a implies b` | Logical implication (equivalent to `!a \|\| b`) |

```kotlin
@AlwaysVerify
fun testImplies(arg: Boolean): Boolean {
    postconditions<Boolean> { res ->
        arg implies !res
        res implies !arg
        !arg implies res
        !res implies arg
    }
    return !arg
}

@AlwaysVerify
fun testAnd(arg1: Boolean, arg2: Boolean): Boolean {
    postconditions<Boolean> { res ->
        res implies (arg1 && arg2)
        !res implies (!arg1 || !arg2)
        (arg1 && arg2) implies res
    }
    return arg1 && arg2
}
```

---

## Chaining calls across specification boundaries

Postconditions of one verified function act as preconditions for its callers. This allows compositional reasoning:

```kotlin
@AlwaysVerify
fun recursiveSumOfIntegersUpToN(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { res -> res == n * (n + 1) / 2 }

    if (n == 0) return 0
    else return n + recursiveSumOfIntegersUpToN(n - 1)
}
```

The verifier uses the postcondition of the recursive call to prove the postcondition of the current call.

```kotlin
@AlwaysVerify
fun returnGreater13(): Int {
    postconditions<Int> { it > 13 }
    return 16
}

@AlwaysVerify
fun subtractTen(int: Int): Int {
    preconditions { int > 10 }
    postconditions<Int> { it > 0 }
    return int - 10
}

// Accepted: returnGreater13() guarantees result > 13 > 10
@AlwaysVerify
fun chainedCall() = subtractTen(returnGreater13())
```

---

## Kotlin contracts integration

SnaKt understands Kotlin's standard `contract { }` DSL and encodes the declared effects into the Viper model. This allows the verifier to exploit smart-cast information and return-value constraints that Kotlin itself infers.

### `returns()` and `returns(value)`

```kotlin
import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
fun returnsTrue(): Boolean {
    contract {
        returns(true)
    }
    return true
}
```

### `returns(value) implies (condition)`

```kotlin
@OptIn(ExperimentalContracts::class)
fun isNonNegative(x: Int): Boolean {
    contract {
        returns(true) implies (x >= 0)
        returns(false) implies (x < 0)
    }
    return x >= 0
}
```

### `returns(null)` and `returnsNotNull()`

```kotlin
@OptIn(ExperimentalContracts::class)
fun returnsNullImpliesInput(x: Boolean?): Boolean? {
    contract {
        returns(null) implies (x == null)
        returnsNotNull() implies (x != null)
    }
    return x
}
```

### Type-narrowing contracts with `is`

```kotlin
@OptIn(ExperimentalContracts::class)
fun isString(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}
```

The verifier uses the contract information to refine the types of values at call sites, mirroring how Kotlin's smart-cast mechanism works. After a call to `isString(x)` that returns `true`, the verifier knows `x` is a `String`.

### Using contracts as the default selection

When `conversionTargetsSelection` (and `verificationTargetsSelection`) is left at the default `targets_with_contract`, functions are automatically converted and verified if they contain a `contract { }` block. This lets you adopt SnaKt incrementally in an existing codebase without annotating every function.
