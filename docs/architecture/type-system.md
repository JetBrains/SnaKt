# Type System

This page describes how SnaKt encodes Kotlin's rich type system in Viper, which has only four base types.

Source: `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/types/`

---

## The mismatch

Viper's type system is minimal:

| Viper type | Meaning |
|---|---|
| `Ref` | Heap object reference (universal object type) |
| `Int` | Arbitrary-precision integer |
| `Bool` | Boolean |
| `Perm` | Permission amount (used only in permission expressions) |

Kotlin's type system is much richer: it has nullable types, smart casts, class hierarchies, function types, and a variety of primitive types (`Int`, `Boolean`, `Char`, `String`).

SnaKt bridges this gap through two mechanisms:

1. **`PretypeEmbedding`** — represents the structural type of a Kotlin value within the Viper `Ref` universe.
2. **`RuntimeTypeDomain`** — a Viper domain (an abstract algebraic structure with axioms) that encodes Kotlin's type hierarchy at the Viper level.

---

## `PretypeEmbedding`

`PretypeEmbedding` is the structural type of a Kotlin value, independent of nullability:

| Pretype | Kotlin type |
|---|---|
| `UnitTypeEmbedding` | `Unit` |
| `NothingTypeEmbedding` | `Nothing` |
| `AnyTypeEmbedding` | `Any` |
| `IntTypeEmbedding` | `Int` |
| `BooleanTypeEmbedding` | `Boolean` |
| `CharTypeEmbedding` | `Char` |
| `StringTypeEmbedding` | `String` |
| `ClassTypeEmbedding` | User-defined class or interface |
| `FunctionTypeEmbedding` | Function type `(A, B) -> C` |

### Type injection for built-ins

Viper has native `Int` and `Bool` types that are more efficient for arithmetic and logical reasoning than encoding everything as `Ref`. For performance, SnaKt uses a **type injection** for `Int`, `Bool`, and `Char`: a pair of Viper domain functions that inject a value from the `Ref` universe into the built-in type and project it back.

For `Int`:
- `inject_Int(ref): Int` — extract the integer value from a `Ref`
- `project_Int(i: Int): Ref` — wrap an integer as a `Ref`

The axiom `inject_Int(project_Int(i)) == i` ensures the round-trip is lossless.

This means that in generated Viper code, integer arithmetic uses Viper's native `Int` operators (`+`, `-`, `*`, `/`, `%`, `<`, `<=`) rather than opaque uninterpreted function calls, which significantly helps Z3 reason about arithmetic properties.

---

## `TypeEmbedding`

`TypeEmbedding` wraps a `PretypeEmbedding` with `TypeEmbeddingFlags`:

| Flag | Meaning |
|---|---|
| `nullable` | The type allows `null` (e.g., `Int?`, `String?`) |
| `unique` | The value is owned exclusively (see [Uniqueness](uniqueness.md)) |

Key methods:
- `getNullable()` / `getNonNullable()` — produce the nullable or non-nullable variant.
- `injection` / `injectionOrNull` — the type injection for built-in pretypes; `null` for `Ref`-based types.
- Invariant queries delegate to the pretype with flag adjustments.

Builder DSLs:
- `buildType { ... }` / `TypeBuilder` — for assembling types from pretypes and flags.
- `buildFunctionPretype { ... }` — for function types.
- `buildClassPretype { ... }` — for class types.

---

## `ClassTypeEmbedding` and `ClassEmbeddingDetails`

Class types are the most complex case. `ClassTypeEmbedding` uses **lazy initialization** to handle forward references: a class embedding is created when its name is first encountered, and its full details (fields, supertypes, predicates) are filled in later.

`ClassEmbeddingDetails` stores:
- `superTypes: List<ClassTypeEmbedding>` — direct supertypes.
- `fields: List<FieldEmbedding>` — fields owned by this class.
- `sharedPredicate` — the Viper predicate that bundles read permissions for shared (non-unique) instances.
- `uniquePredicate` — the Viper predicate for exclusively-owned instances.

Key methods on `ClassTypeEmbedding`:
- `hierarchyUnfoldPath()` — computes the sequence of predicates that must be unfolded to reach a field through the inheritance hierarchy.
- `accessInvariants()` — assembles the full set of field access requirements needed for Viper to verify field accesses on instances of this class.

---

## `RuntimeTypeDomain`

The `RuntimeTypeDomain` is a Viper domain that axiomatizes Kotlin's type hierarchy. It provides:

- A `type` function mapping `Ref` values to their runtime type token.
- Subtype axioms encoding the `is` relationship: if `type(x) == T` and `T <: S`, then `type(x) == S` also holds.
- Axioms for class constants: each class has a unique type token.

This allows SnaKt to encode `x is T` as a Viper assertion about `type(x)`, which Z3 can reason about using the axioms.

---

## Type invariants

`TypeInvariantEmbedding` represents a single constraint that must hold for a value of a given type:

| Invariant | Meaning |
|---|---|
| `SubTypeInvariantEmbedding` | The value must pass an `is T` check |
| `FieldAccessTypeInvariantEmbedding` | A specific field permission must be held for this value |
| `PredicateAccessTypeInvariantEmbedding` | A specific predicate permission must be held |
| `FieldEqualsInvariant` | A field must equal a specific constant value |
| `IfNonNullInvariant` | The inner condition applies only when the value is non-null |
| `FalseTypeInvariant` | Unsatisfiable constraint — used for the `Nothing` type |

`TypeInvariantHolder` is the interface for anything that can supply invariants:
- `accessInvariants()` — invariants about heap permissions (used in method pre/postconditions).
- `pureInvariants()` — invariants expressible as pure boolean constraints.
- Predicate invariants — invariants involving Viper predicates.

---

## Nullability encoding

In Viper, `null` is a special `Ref` value (the null reference). Nullable Kotlin types are represented as `Ref` values that might be equal to this null constant.

When SnaKt encounters a field access on a nullable receiver (e.g., `x?.field`), it generates:
- A null check: an assertion or conditional that `x != null`.
- The field access under that guard.

For `@Nullable` parameters with type invariants, `IfNonNullInvariant` wraps the invariants so that field permission requirements only apply to non-null values. This accurately models Kotlin's null safety: you do not need read permission on fields of a null reference because you cannot access them.

---

## Smart casts

Kotlin's smart cast system allows the compiler to narrow a type after an `is`-check:

```kotlin
if (x is String) {
    // x is treated as String here
    println(x.length)
}
```

SnaKt encodes this by:
1. Translating `x is String` as a runtime type check using the `RuntimeTypeDomain`.
2. On the true branch, emitting a `Cast` embedding that annotates `x` with the narrower type `String`.
3. Contract effects like `returns() implies (x is T)` use `SubTypeInvariantEmbedding` to propagate type information into callers' Viper verification conditions.

This means that after a type-narrowing contract, the caller's Viper proof state includes the knowledge that `x`'s runtime type is a subtype of `T`, allowing field accesses and method calls on `x` to be verified without additional dynamic checks.
