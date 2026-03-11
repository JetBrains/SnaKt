# Annotations & Built-ins

All SnaKt annotations and specification functions are in the `org.jetbrains.kotlin.formver.plugin` package.

```kotlin
import org.jetbrains.kotlin.formver.plugin.*
```

---

## Quick reference

| Name | Kind | Purpose |
|------|------|---------|
| `@AlwaysVerify` | Annotation | Verify this function regardless of project settings |
| `@NeverVerify` | Annotation | Convert to Viper but skip verification |
| `@NeverConvert` | Annotation | Exclude from Viper conversion entirely |
| `@DumpExpEmbeddings` | Annotation | Debug: dump the intermediate expression embedding |
| `@Pure` | Annotation | Assert the function has no side effects |
| `@Unique` | Annotation | The annotated value has unique (exclusive) ownership |
| `@Borrowed` | Annotation | The function borrows a unique value without consuming it |
| `@Manual` | Annotation | Opt a property out of automatic permission management |
| `verify(...)` | Function | Assert that expressions hold at a specific point |
| `preconditions { }` | Function | Declare function preconditions |
| `postconditions<T> { }` | Function | Declare function postconditions |
| `loopInvariants { }` | Function | Declare loop invariants |
| `forAll<T> { }` | Function | Universal quantification over all values of type `T` |
| `triggers(...)` | Function | Provide explicit SMT triggers inside `forAll` |
| `implies` | Infix operator | Logical implication (`a implies b` ≡ `!a || b`) |

---

## Verification control annotations

### Summary

| Annotation | Converted | Verified |
|-----------|-----------|---------|
| _(none — default)_ | If covered by project setting | If covered by project setting |
| `@AlwaysVerify` | Yes | Yes, always |
| `@NeverVerify` | Yes | No |
| `@NeverConvert` | No | No |

### `@AlwaysVerify`

Verifies the function regardless of the project-level `verificationTargetsSelection` setting. Use this on any function you want the plugin to check, even when the project default is `targets_with_contract` or `no_targets`.

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

### `@NeverVerify`

Converts the function to Viper but skips the verification step. Useful when you want to inspect the generated Viper code (with `logLevel("full_viper_dump")`) without paying the cost of running Silicon.

```kotlin
@NeverVerify
fun inspectOnly(x: Int): Int {
    postconditions<Int> { it > x }
    return x + 1
}
```

### `@NeverConvert`

Excludes the function from Viper conversion entirely. No Viper code is generated for it, and it cannot be referenced in specifications. Use this for helper functions that perform I/O, logging, or other side effects that are irrelevant to the verification of surrounding code.

```kotlin
@NeverConvert
fun logResult(result: Int) {
    println("Result: $result")
}
```

---

## `@DumpExpEmbeddings`

Emits the intermediate expression embedding (the plugin's internal IR) as a compiler info diagnostic. This is a low-level debugging tool for plugin developers who want to inspect how a function's body was translated before linearization.

```kotlin
@AlwaysVerify
@DumpExpEmbeddings
fun debugMe(x: Int): Int {
    postconditions<Int> { it == x + 1 }
    return x + 1
}
```

Run with `./gradlew compileKotlin --info` to see the `EXP_EMBEDDING` output.

---

## `@Pure`

Marks a function as side-effect-free. Pure functions can be called inside specification blocks (`preconditions`, `postconditions`, `loopInvariants`) and inside `verify()`. They are translated to Viper `function` declarations (which can appear in assertions) rather than Viper `method` declarations.

```kotlin
@Pure
fun square(x: Int): Int = x * x

@Pure
fun isPositive(x: Int): Boolean = x > 0

@AlwaysVerify
fun testPure(x: Int) {
    preconditions { isPositive(x) }
    postconditions<Unit> { square(x) > 0 }
}
```

Calling a non-pure function inside a `@Pure` function is a compile-time `INTERNAL_ERROR`. Mutation operators such as `x++` used inside `verify()` produce a `PURITY_VIOLATION` diagnostic.

```kotlin
// ERROR: INTERNAL_ERROR — calls a non-pure function
@Pure
fun badPure(): Int {
    return nonPureHelper()
}

@AlwaysVerify
fun badVerify() {
    var x = 42
    verify(x++ < 43)  // ERROR: PURITY_VIOLATION
}
```

---

## `@Unique` and `@Borrowed`

SnaKt includes a lightweight ownership checker. Enable it with `checkUniqueness(true)` in your `formver { }` block, or add `// UNIQUE_CHECK_ONLY` at the top of a file to run only the ownership check.

### `@Unique`

Can be applied to function parameters, function return values, and properties. A `@Unique` value has a single owner and is **consumed** (moved) when passed to a function that expects a `@Unique` parameter.

```kotlin
class Box(@Unique val contents: Any)

// consumeBox takes ownership of `box`; the caller cannot use it afterwards
fun consumeBox(@Unique box: Box) { /* ... */ }

@Unique
fun makeBox(): Box = Box(42)

fun example() {
    val b = makeBox()   // b is unique
    consumeBox(b)       // ownership transferred
    consumeBox(b)       // ERROR: UNIQUENESS_VIOLATION — b already consumed
}
```

`@Unique` on a property field transfers ownership when the field is accessed:

```kotlin
class Container {
    @Unique val item = Box(1)
}

fun consume(@Unique b: Box) {}
fun consumeContainer(@Unique c: Container) {}

fun partialMove(@Unique c: Container) {
    consume(c.item)          // consumes the @Unique field
    consumeContainer(c)      // ERROR: UNIQUENESS_VIOLATION — c has been partially moved
}
```

### `@Borrowed`

Allows passing a `@Unique` value to a function without consuming it, so the caller retains ownership after the call returns.

```kotlin
// borrow does not take ownership
fun borrow(@Borrowed box: Box) { /* read-only use */ }

fun example(@Unique b: Box) {
    borrow(b)           // b is lent, not consumed
    consumeBox(b)       // OK — b is still owned here
}
```

Passing a `@Borrowed` parameter to a consuming (non-`@Borrowed`) function is an error:

```kotlin
fun badBorrow(@Borrowed @Unique b: Box) {
    consumeBox(b)  // ERROR: UNIQUENESS_VIOLATION — cannot consume a borrowed value
}
```

---

## `@Manual`

Applied as `@property:Manual`, this opts a single property out of the automatic Viper access-predicate system. The programmer is responsible for providing access predicates for that field manually. Files that use `@Manual` typically include `// NEVER_VALIDATE` at the top.

```kotlin
import org.jetbrains.kotlin.formver.plugin.Manual

class Fields(
    val a: Int,
    @property:Manual var b: Int,  // managed manually
)

fun readFields(f: Fields) {
    val a = f.a   // automatic permission
    val b = f.b   // manual permission
}
```

---

## Specification functions

### `verify(...)`

Asserts that one or more boolean expressions hold at the current program point. Each argument is checked independently. Arguments must be pure expressions.

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

`verify()` can be used independently of `preconditions` and `postconditions` and does not require `@AlwaysVerify` by itself (the surrounding function still needs to be converted).

---

### `preconditions { }`

Declares conditions that must hold when the function is called. Each statement in the block is a boolean expression. The verifier assumes these hold inside the function body and checks that every call site satisfies them.

```kotlin
@AlwaysVerify
fun safeDivide(a: Int, b: Int): Int {
    preconditions {
        b != 0
    }
    return a / b
}
```

Multiple conditions are implicitly conjoined. `preconditions { }` must appear at the top of the function body, before any other statements.

---

### `postconditions<T> { result -> ... }`

Declares conditions the function guarantees on return. The type parameter `T` is the return type; the lambda parameter names the return value. `it` may be used as shorthand when the parameter is not named.

```kotlin
@AlwaysVerify
fun addFive(x: Int): Int {
    postconditions<Int> { result ->
        result == x + 5
        result > x
    }
    return x + 5
}

// Using `it` shorthand
@AlwaysVerify
fun positive(): Int {
    postconditions<Int> { it > 0 }
    return 1
}
```

Postconditions of a called function are available to its callers as preconditions. This allows chaining verified calls:

```kotlin
@AlwaysVerify
fun returnGreater13(): Int {
    postconditions<Int> { it > 13 }
    return 16
}

@AlwaysVerify
fun subtractTen(n: Int): Int {
    preconditions { n > 10 }
    postconditions<Int> { it > 0 }
    return n - 10
}

// The verifier accepts this because returnGreater13() guarantees result > 13 > 10
@AlwaysVerify
fun chainedCall() = subtractTen(returnGreater13())
```

---

### `loopInvariants { }`

Declares loop invariants for the enclosing `while` loop. Must appear at the top of the loop body, before any other statements. Each statement is a boolean expression.

Rules:
- The invariant must hold when the loop is first entered.
- The loop body may assume the invariant holds at the top of each iteration.
- The invariant must be re-established at the end of every iteration.
- After the loop exits, the invariant holds and the loop condition is false.

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

### `forAll<T> { }`

Expresses a universally quantified property over all values of type `T`. Used inside `preconditions`, `postconditions`, and `loopInvariants` blocks.

```kotlin
@AlwaysVerify
fun example(arr: IntArray): Unit {
    preconditions {
        forAll<Int> { j ->
            (0 <= j && j < arr.size()) implies (arr[j] > 0)
        }
    }
}

fun anyIntegerSquaredNonNegative(): Int {
    postconditions<Int> { res ->
        forAll<Int> {
            it * it >= 0
            it * it >= res
        }
    }
    return 0
}
```

---

### `triggers(...)`

Provides explicit SMT trigger expressions for the enclosing `forAll`. Call `triggers()` as the first statement inside the `forAll` block. Each argument becomes a separate trigger expression.

When the SMT solver encounters a ground instance of the trigger expression, it instantiates the quantifier. Without triggers, the solver infers them automatically, which may fail for complex quantifiers.

```kotlin
// Single trigger
fun withSimpleTrigger(): Int {
    postconditions<Int> { res ->
        forAll<Int> {
            triggers(it * it)
            it * it >= 0
            it * it >= res
        }
    }
    return 0
}

// Multiple trigger expressions
fun withMultipleTriggers(): Int {
    postconditions<Int> { res ->
        forAll<Int> {
            triggers(it * it, it + 1)
            (it != 0) implies (it * it >= res)
        }
    }
    return 1
}
```

Each argument to `triggers()` becomes a separate trigger. This differs from Viper's native syntax, which supports grouping multiple expressions in a single trigger; currently SnaKt only supports simple (single-expression) triggers.

---

### `implies` infix operator

Logical implication. `a implies b` is equivalent to `!a || b`. Available inside and outside specification blocks.

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
```

Use it in quantifiers to express conditional properties:

```kotlin
forAll<Int> {
    (it != 0) implies (it * it > 0)
}
```
