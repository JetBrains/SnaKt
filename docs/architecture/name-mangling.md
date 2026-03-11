# Name Mangling

> There are two hard problems in computer science: cache invalidation, naming, and off-by-one errors.

When translating Kotlin to Viper, every Kotlin entity — classes, functions, parameters, local variables, labels — needs a corresponding Viper name. Viper is significantly less forgiving than Kotlin about name shadowing: the same name cannot be reused in different scopes within the same program. SnaKt therefore derives all Viper names through a systematic mangling scheme that produces **globally unique identifiers**.

Source: `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/names/`

---

## The problem

Kotlin allows (and encourages) concise names that are scoped and may shadow outer names:

```kotlin
fun foo(x: Int): Int {
    val x = x + 1   // local x shadows parameter x
    return x
}
```

Viper requires that every local variable and parameter in a method has a distinct name within that method. Furthermore, types, fields, predicates, domains, and functions share a single global namespace across the whole program.

SnaKt must therefore produce names that are:
1. **Unique** within their Viper scope.
2. **Deterministic** — the same Kotlin construct always produces the same Viper name, so that incremental compilation and debugging are predictable.
3. **Human-readable** — developers reading the Viper dump should be able to recognise the original Kotlin entity.

---

## Solution: scope-qualified, type-annotated names

Every name is given two qualifiers:

1. **A scope prefix** — indicating roughly where in the Kotlin source the entity lives.
2. **A type abbreviation** — indicating what kind of thing the name refers to.

Because these qualifiers typically make names long, SnaKt uses short abbreviations and may omit the type or scope for the most common cases.

---

## Scope abbreviations

| Abbreviation | Meaning |
|---|---|
| `pkg` | Package-level entity |
| `g` | Global (top-level, not package-qualified) |
| `p` | Function parameter |
| `l1`, `l2`, ... `ln` | Local scope, where `n` is the depth index of the enclosing block |

---

## Type abbreviations

| Abbreviation | Refers to |
|---|---|
| `c` | Class |
| `f` | Function |
| `d` | Domain |
| `lbl` | Label (for `break`/`continue`/`return` targets) |
| `anon` | Anonymous (compiler-generated) value |
| `ret` | Return value placeholder |
| `con` | Constructor |
| `pp` | Property (internal use only) |
| `bf` | Backing field |
| `pg` | Property getter |
| `ps` | Property setter |
| `eg` | Extension getter |
| `es` | Extension setter |
| `p` | Predicate |

---

## Examples

A function `myFunction` in package `com.example` becomes something like:

```
pkg_com_example_f_myFunction
```

A parameter `count` of that function becomes:

```
p_count
```

A local variable `result` in the outermost block of a function body becomes:

```
l1_result
```

If `result` is shadowed by an inner block, the inner variable becomes:

```
l2_result
```

A backing field for property `name` on class `Person`:

```
pkg_com_example_c_Person_bf_name
```

The shared predicate for `Person`:

```
pkg_com_example_c_Person_p_shared
```

---

## Implementation

The mangling logic is implemented in the `names/` package:

### `ScopedKotlinName`

`ScopedKotlinName` is a data class that combines a `NameScope` with a type-abbreviation tag and the original Kotlin identifier string. It provides a `toViperName()` method that assembles the final mangled string.

### `NameScope`

`NameScope` is a sealed interface hierarchy representing the scope levels:

- `PackageScope(packageFqName)` — for package-level declarations.
- `GlobalScope` — for top-level declarations without a package.
- `ParameterScope` — for function parameters.
- `LocalScope(depth)` — for local variables, with a depth counter.

### `FreshEntityProducer`

For entities that need globally fresh names (SSA variables, anonymous temporaries, synthetic labels), `FreshEntityProducer<T, K>` maintains a counter per source key. Each call to `getFresh(key)` increments the counter and returns a new `T` with a unique name derived from the key and counter value.

---

## Current limitations

The mangling scheme is a work in progress. There are still holdover longer names in some places. Ideally the system would be modular and configurable — dropping prefixes when they are redundant, for example when a package has only one class — but that refinement is not yet implemented. All names produced today are correct and unique; they are simply not always as short as they could be.
