# Supported Features

This page lists which Kotlin features are supported, partially supported, or not yet supported in verified code. SnaKt is in early development; this list reflects the current state of the plugin.

---

## Control flow

### Fully supported

**`if` / `else`**

```kotlin
fun abs(x: Int): Int = if (x >= 0) x else -x
```

**`when`** (both expression and statement forms)

```kotlin
fun classify(x: Int): Int = when {
    x < 0  -> -1
    x == 0 -> 0
    else   -> 1
}
```

**`while` with `break` and `continue`**

```kotlin
fun firstNegative(arr: IntArray): Int {
    var i = 0
    while (i < arr.size) {
        if (arr[i] < 0) break
        i++
    }
    return i
}
```

**`for` loops**

`for` loops over integer ranges and collections are supported. They are internally desugared to `while` loops.

**`try` / `catch`**

```kotlin
fun safe(x: Int): Int {
    return try { x / 1 } catch (e: Exception) { 0 }
}
```

### Not yet supported

- `do...while` loops
- `finally` blocks in `try/catch/finally`
- Non-local returns from non-inline lambdas

---

## Operators

| Category | Supported operators |
|----------|---------------------|
| Arithmetic | `+`, `-`, `*`, `/`, `%` (remainder) |
| Unary arithmetic | Unary `-` (negation) |
| Comparison | `<`, `<=`, `>`, `>=`, `==`, `!=` |
| Boolean | `&&`, `\|\|`, `!` |
| Increment / Decrement | `++`, `--` (pre and post) |
| Compound assignment | `+=`, `-=`, `*=`, `/=` |
| Type checks | `is`, `!is`, `as`, `as?` |
| Range | `in`, `!in` (for integer ranges) |
| Null-safe | `?.` (safe call), `?:` (Elvis) |

### Not yet supported

- Bitwise operators (`shl`, `shr`, `ushr`, `and`, `or`, `xor`, `inv`)
- Operator overloading for user-defined types (beyond the built-in types)

---

## Classes and interfaces

### Supported

- Primary and secondary constructors
- Single-class inheritance (`open class`)
- Multiple interface implementation
- Property getters and setters with backing fields
- Extension properties
- Member functions and function overloading
- `data class` (conversion supported; some features limited — see below)
- `sealed class` (partial support)
- `object` declarations

```kotlin
open class Shape(val area: Int)

interface Drawable {
    val color: String
}

class Circle(area: Int, override val color: String) : Shape(area), Drawable
```

### Partially supported

**Data classes:** The generated `equals`, `hashCode`, `copy`, and `toString` methods are not verified. The primary constructor and property accessors work normally.

**Sealed classes:** The class hierarchy is modelled but exhaustiveness checking in `when` expressions on sealed types may not be fully exploited by the verifier.

### Not yet supported

- `enum class`
- Delegation (`by` keyword)
- Companion objects with complex state
- Inner classes

---

## Lambdas and inline functions

**Inline functions** whose lambda parameters are expanded at call sites are fully supported. Non-local returns from inline lambdas are handled correctly.

```kotlin
inline fun <T> myRun(block: () -> T): T = block()

@AlwaysVerify
fun useMyRun(x: Int): Int {
    return myRun { x + 1 }
}
```

**Non-inline lambdas** (lambdas stored in variables, passed to non-inline functions, or called later) are not currently supported in verified functions.

---

## Generics

Basic generic classes and functions are supported:

```kotlin
class Box<T>(val value: T)

fun <T> identity(x: T): T = x
```

### Not yet supported

- Generic constraints with multiple bounds (`where T : A, T : B`)
- Variance annotations (`in`, `out`) in specifications
- Reified type parameters

---

## Nullability

Nullable types (`T?`), null checks, null-safe calls (`?.`), Elvis operator (`?:`), and smart-casts through null checks are fully modelled.

```kotlin
@AlwaysVerify
fun safeHead(list: List<Int>?): Int? {
    if (list != null && !list.isEmpty()) {
        return list[0]
    }
    return null
}
```

---

## Standard library

When the `// REPLACE_STDLIB_EXTENSIONS` file-level directive is active, the following standard library functions are handled by SnaKt's built-in models:

| Function | Modelled as |
|----------|------------|
| `check(condition)` | Verified assertion (`assert` in Viper) |
| `run { }` | Inline lambda expansion |
| `x.run { }` | Inline lambda expansion with `this` bound to `x` |
| `x.let { }` | Inline lambda expansion with `it` bound to `x` |
| `x.also { }` | Inline lambda expansion with `it` bound to `x`; returns `x` |
| `with(x) { }` | Inline lambda expansion with `this` bound to `x` |
| `x.apply { }` | Inline lambda expansion with `this` bound to `x`; returns `x` |

```kotlin
// REPLACE_STDLIB_EXTENSIONS

fun useStdlib(x: Int) {
    check(x > 0)

    val result = x.let { it + 1 }
    verify(result == x + 1)

    x.also { verify(it == x) }

    with(x) { verify(this == x) }
}
```

### Scoped receiver disambiguation

When multiple `with`/`run` blocks are nested, use `this@label` to refer to the correct receiver:

```kotlin
fun nestedReceivers(x: Int) {
    with(true) {
        false.run {
            verify(
                !this,      // false (innermost run receiver)
                this@with,  // true (outer with receiver)
            )
        }
    }
}
```

---

## Strings

SnaKt models `String` as a sequence of `Char` values.

| Operation | Notes |
|-----------|-------|
| `s.length` | Supported |
| `s[i]` | Requires `0 <= i < s.length`; verifier checks bounds |
| `s[i] == 'a'` | Character comparison |
| `s[i] < c` | Character ordering |
| `s + t` | String concatenation |
| String literals | `"abc"[0] == 'a'` is provable |
| Character arithmetic | `s[i] - 'a'` returns an `Int` |

```kotlin
@AlwaysVerify
fun testStrings(s: String) {
    verify("str".length == 3)
    verify("Kotlin" + "." + "String" == "Kotlin.String")
    val str = "aba"
    verify(str[0] == str[2])
    verify(str[1] == 'b')
}
```

---

## Lists

SnaKt supports `List<T>` and `MutableList<T>`:

| Operation | Notes |
|-----------|-------|
| `l.size` | Number of elements |
| `l.isEmpty()` | True if `l.size == 0` |
| `l[i]` | Requires `0 <= i < l.size`; verifier checks bounds |
| `l.add(x)` | Appends to a `MutableList` |
| `emptyList<T>()` | Creates an empty list |

```kotlin
@AlwaysVerify
fun lastOrNull(l: List<Int>): Int? {
    val size = l.size
    return if (size != 0) l[size - 1] else null
}

@AlwaysVerify
fun addAndGet(l: MutableList<Int>) {
    l.add(1)
    val n = l[0]
}
```

---

## SMT triggers

Manual SMT triggers can be specified inside `forAll` blocks using `triggers(...)`. See [Writing Specifications — SMT triggers](specifications.md#smt-triggers) for details and examples.

---

## Manual permissions

The `@Manual` annotation opts a property out of the automatic Viper access-predicate system. This is an advanced feature for interoperating with code that manages its own heap permissions. See [@Manual](annotations.md#manual) for details.

---

## Kotlin contracts

The full `contract { }` DSL is supported, including `returns()`, `returns(value)`, `returns(value) implies (condition)`, `returnsNotNull()`, and type-narrowing `is` conditions. See [Kotlin contracts integration](specifications.md#kotlin-contracts-integration) for details and examples.

---

## Not yet supported

The following Kotlin features are not currently supported in verified functions. If the plugin encounters them with the default `unsupportedFeatureBehaviour("throw_exception")` setting, it will abort conversion and report an `INTERNAL_ERROR`. You can use `unsupportedFeatureBehaviour("assume_unreachable")` to skip over them.

- **Coroutines** — `suspend` functions, `async`/`await`, `Flow`
- **Reflection** — `KClass`, `KFunction`, `::reference`
- **File I/O and system calls** — anything in `java.io`, `java.nio`, etc.
- **Complex generic constraints** — `where` clauses, contravariant/covariant type parameters in specifications
- **Destructuring declarations** — `val (a, b) = pair` in some contexts
- **`do...while` loops**
- **`finally` blocks** in exception handling
- **Non-inline higher-order functions** — lambdas stored in variables or passed to non-inline functions
- **Bitwise operators** — `shl`, `shr`, `ushr`, `and`, `or`, `xor`, `inv`
- **`enum class`** and **`object` expressions** (anonymous objects)
- **Delegation** — `by` keyword
- **Many standard library functions** not listed in the supported set above
