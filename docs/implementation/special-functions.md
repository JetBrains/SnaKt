# Special Functions

Many Kotlin standard library operations have no meaningful Viper method representation. For example, `Int.plus(Int)` is just Viper integer addition — generating a Viper method call would be incorrect and would make verification impossible. SnaKt intercepts these operations during conversion and substitutes the appropriate Viper built-in expression directly.

The special-functions registry lives in `formver.compiler-plugin/core/src/…/core/embeddings/callables/`.

---

## Why special handling is needed

Kotlin standard library functions like `Int.plus()`, `Boolean.not()`, or `String.length` map directly to Viper built-in operators or field accesses. If the plugin tried to generate Viper method calls for these, Silicon would not know how to verify them.

Similarly, higher-order scope functions (`run`, `let`, `also`, `with`, `apply`) must be inlined rather than called as methods, because Viper has no concept of function objects.

---

## Two-tier system

The special-functions system distinguishes two tiers:

### FullySpecialKotlinFunction

A `FullySpecialKotlinFunction` is always intercepted, regardless of context. At the call site, it is replaced with a custom `ExpEmbedding` produced by its `body` lambda:

```kotlin
interface FullySpecialKotlinFunction : FunctionEmbedding {
    val body: (List<ExpEmbedding>, StmtConversionContext) -> ExpEmbedding
    fun insertCall(args, ctx): ExpEmbedding = body(args, ctx)
}
```

Fully-special functions are registered in the `methods` map of `ProgramConverter` at initialization time, before any user code is processed. If the plugin encounters a call to one of these functions, the registry lookup returns the special embedding and no Viper method call is generated.

### PartiallySpecialKotlinFunction

A `PartiallySpecialKotlinFunction` is special in some cases but falls back to a regular Viper method call in others. The decision is made at the call site based on argument types.

Example: `String.plus(Any?)`:
- If the argument is a `String`, emit `AddStringString` (Viper sequence append).
- If the argument is a `Char`, emit `AddStringChar`.
- Otherwise, fall back to a regular Viper method call.

The `PartiallySpecialKotlinFunction` holds an optional `baseEmbedding` (the regular Viper method). When the fallback path is needed, `ProgramConverter.embedFunction` injects the user embedding as `baseEmbedding` on first encounter.

---

## FullySpecialKotlinFunctionBuilder DSL

Fully-special functions are defined using `buildFullySpecialFunctions { ... }`, a DSL provided by `FullySpecialKotlinFunctionBuilder`:

```kotlin
buildFullySpecialFunctions {
    withCallableType(intBinaryOpType) {
        addFunction(
            packageName = listOf("kotlin"),
            className = "Int",
            name = "plus",
        ) { args, _ -> AddIntInt(args[0], args[1]) }
    }
}
```

Each entry specifies:
- `packageName`: the Kotlin package (e.g. `listOf("kotlin")`).
- `className`: the receiver class (e.g. `"Int"`), or `null` for top-level functions.
- `name`: the function name.
- A lambda `(List<ExpEmbedding>, StmtConversionContext) -> ExpEmbedding` producing the replacement embedding.

`addNoOpFunction` is a convenience for functions that should produce `UnitLit` (e.g. no-op stdlib functions that have no Viper equivalent).

---

## Complete list of always-special mappings

### Integer arithmetic

| Kotlin | Viper embedding |
|---|---|
| `Int.plus(Int)` | `AddIntInt` (Viper `+`) |
| `Int.minus(Int)` | `SubIntInt` (Viper `-`) |
| `Int.times(Int)` | `MulIntInt` (Viper `*`) |
| `Int.div(Int)` | `DivIntInt` (Viper `/`) |
| `Int.rem(Int)` | `RemIntInt` (Viper `%`) |
| `Int.unaryMinus()` | `UnaryMinusInt` (Viper unary `-`) |

### Integer comparisons

| Kotlin | Viper embedding |
|---|---|
| `Int.compareTo(Int)` | `CompareIntInt` (domain function) |
| `<`, `<=`, `>`, `>=` on `Int` | `LtCmp`, `LeCmp`, `GtCmp`, `GeCmp` |

### Boolean operators

| Kotlin | Viper embedding |
|---|---|
| `Boolean.not()` | `NotBool` (Viper `!`) |
| `Boolean.and(Boolean)` | `AndBool` (Viper `&&`) |
| `Boolean.or(Boolean)` | `OrBool` (Viper `\|\|`) |

### Character arithmetic and comparisons

Characters are encoded as `Int` in Viper (Unicode code points). All `Char` arithmetic and comparison operations map to the corresponding integer operations.

| Kotlin | Viper embedding |
|---|---|
| `Char.plus(Int)` | `AddCharInt` |
| `Char.minus(Char)` | `SubCharChar` |
| `Char.minus(Int)` | `SubCharInt` |
| `Char.compareTo(Char)` | `CompareCharChar` |
| `Char.code` | identity (already an integer) |

### String operations

Kotlin `String` is encoded as `Seq[Int]` in Viper (a sequence of character code points).

| Kotlin | Viper embedding |
|---|---|
| `String.length` | `SeqLength` (Viper `\|s\|`) |
| `String.get(Int)` | `SeqIndex` (Viper `s[i]`) |
| `String.plus(String)` | `AddStringString` (Viper `s ++ t`) |
| `String.plus(Char)` | `AddStringChar` |

---

## PartiallySpecialKotlinFunction: String.plus(Any?)

`String.plus(Any?)` is partially special because its behaviour depends on the dynamic type of the argument:

- Argument is `String`: emit `AddStringString`.
- Argument is `Char`: emit `AddStringChar`.
- Otherwise: fall back to a regular Viper method call that the user must specify.

The dispatching logic lives in the `insertCall` implementation of the `PartiallySpecialKotlinFunction` for `String.plus`.

---

## SpecialMethods

`SpecialMethods` is analogous to `SpecialFunctions` but for method-style calls. Currently this includes:

- `check(Boolean)` → translated to a Viper `Assert` statement.

These are emitted directly as Viper statements rather than expressions.

---

## SpecialFields

`SpecialFields` contains Viper field declarations for properties that need special treatment (e.g. internal fields used in Viper predicate encodings). These are included in the `Program.fields` list via `SpecialFields.all`.

---

## How the registry is used during conversion

During `StmtConversionVisitor`'s handling of a `FirFunctionCall`:

1. The call's callee symbol is looked up in `ProgramConverter.methods` (impure) or `ProgramConverter.functions` (pure).
2. If the result is a `FullySpecialKotlinFunction`, its `insertCall(args, ctx)` is called immediately and the result is used as the embedding.
3. If the result is a `PartiallySpecialKotlinFunction`, `insertCall` is called; it internally checks argument types and either produces a special embedding or delegates to its `baseEmbedding`.
4. If no registry entry exists, a new `UserFunctionEmbedding` is created and registered.

This means special mappings take priority over any user-defined functions with conflicting names, since the registry is pre-populated before user code is processed.

---

## Key source files

| File | Role |
|---|---|
| `FullySpecialKotlinFunctionBuilder.kt` | DSL for defining always-special function mappings |
| `SpecialFunctions.kt` | Registry of all always-special functions (delegates to `OperatorExpEmbeddings`) |
| `SpecialKotlinFunctions.kt` | `byName` map pre-populated with fully-special entries |
| `PartiallySpecialKotlinFunctions.kt` | Partially-special functions (e.g. `String.plus`) |
| `SpecialMethods.kt` | Special method-style calls (e.g. `check()`) |
| `SpecialFields.kt` | Built-in Viper field declarations |
