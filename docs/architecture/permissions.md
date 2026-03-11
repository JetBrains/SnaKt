# Permissions and Separation Logic

This page describes how SnaKt uses Viper's separation logic permission model to reason about heap accesses.

Source: `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/AccessPolicy.kt`

---

## Separation logic basics

Viper is based on **implicit dynamic frames**, a form of separation logic. The core idea is: to read or write a heap location, you must hold a **permission token** for that location. Permissions:

- Are **exclusive**: write permission to a location means no other thread or method path can access it simultaneously.
- Are **fractional**: permission amounts range from 0 (none) to 1 (full write access), with any positive fraction granting read access.
- Are **tracked by the verifier**: Silicon checks that the required permissions are held at every heap access.

This model enables sound reasoning about aliasing and mutation without requiring global alias analysis.

---

## Viper permission amounts

| Amount | Name | Meaning |
|---|---|---|
| `write` (= 1) | Full permission | Exclusive read + write access |
| `rd` (wildcard) | Read permission | Non-exclusive read-only access; multiple readers can coexist |
| `none` (= 0) | No permission | No access allowed |

In SnaKt, Kotlin's non-unique objects use `rd` (shared read) access, while `@Unique` values use `write` (exclusive) access.

---

## How SnaKt inserts permissions automatically

SnaKt automatically generates the required permission assertions before each heap access. The logic is centralized in `AccessPolicy`.

### `AccessPolicy`

`AccessPolicy` is an interface on `FieldEmbedding` that determines:
- What permission level is required to read or write this field.
- Whether type invariants need to be unfolded before access.

The standard implementations are:

| Policy | Behavior |
|---|---|
| `SharedAccessPolicy` | Read access (`rd`) for reads; write access for writes. Invariants are unfolded via the shared predicate. |
| `UniqueAccessPolicy` | Full write access (`write`) for all accesses. Invariants are unfolded via the unique predicate. |
| `ManualAccessPolicy` | No automatic permission management (used with `@Manual`). |

### Read access

Before a field read, SnaKt emits:

```viper
inhale acc(x.field, rd)
```

This asserts that the caller gains read permission for the duration of the read, and releases it afterwards. For `@Unique` fields, `write` is used instead of `rd`.

### Write access

Before a field write, SnaKt emits:

```viper
inhale acc(x.field, write)
```

Write permission is exclusive — it ensures no other part of the program can read or write the same field concurrently.

---

## Class predicates

Individual field permissions can be bundled into **class predicates** that represent the combined permissions for all fields of an object instance. This is more compositional: you can pass a predicate reference rather than listing every field.

Each class gets up to two predicates:

| Predicate | Used for |
|---|---|
| `sharedPredicate` | Instances with shared (non-unique) ownership. Bundles `rd` permissions for all fields. |
| `uniquePredicate` | Exclusively-owned (`@Unique`) instances. Bundles `write` permissions for all fields. |

The predicates are defined in `ClassEmbeddingDetails` and emitted as Viper `predicate` declarations.

### Method pre/postconditions

When a method takes an object as a parameter, its precondition includes access to the appropriate predicate:

```viper
method doSomething(self: Ref)
  requires acc(shared_MyClass(self), rd)
  ensures  acc(shared_MyClass(self), rd)
{
  ...
}
```

This contract says: the caller must hold (at least) read access to `self`'s shared predicate before calling, and the method guarantees that the same access is still held after returning.

---

## Folding and unfolding

A Viper predicate wraps the permissions for all the fields it covers. Before accessing an individual field behind a predicate, the predicate must be **unfolded** to expose the field permissions. After the access, it must be **refolded** to restore the bundled form.

SnaKt generates these fold/unfold operations automatically during linearization:

```viper
unfold acc(shared_MyClass(self), rd)
// field access here
fold acc(shared_MyClass(self), rd)
```

For inherited fields, `hierarchyUnfoldPath()` on `ClassTypeEmbedding` computes the sequence of predicates that must be unfolded through the inheritance hierarchy to reach the field.

---

## The `@Manual` annotation

Marking a property with `@Manual` opts it out of automatic permission management. SnaKt will not generate `acc(...)` assertions, fold, or unfold operations for `@Manual` fields. This is an advanced escape hatch for properties whose permission model cannot be captured by the standard policies — for example, ghost fields used only in specifications.

---

## `TypeInvariantHolder`

`TypeInvariantHolder` is the interface for anything (type embedding, field embedding, class) that can supply invariants to the verifier:

| Method | Returns |
|---|---|
| `accessInvariants()` | Invariants about heap permissions — expressed as `acc(...)` assertions |
| `pureInvariants()` | Invariants expressible as pure boolean formulas — no permissions needed |

Both categories feed into:
- **Preconditions**: what the caller must provide.
- **Postconditions**: what the callee guarantees.
- **Loop invariants**: what must hold at every loop iteration.

When a class has invariants, SnaKt folds them into the method signatures automatically, so callers and implementations are checked to maintain them without manual annotation.
