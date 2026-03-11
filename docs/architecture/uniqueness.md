# Uniqueness and Ownership

This page describes SnaKt's ownership tracking system, which uses the `@Unique` and `@Borrowed` annotations to enforce linear ownership of object references.

Source: `formver.compiler-plugin/uniqueness/src/org/jetbrains/kotlin/formver/uniqueness/`

---

## Motivation

Shared mutable state is a common source of bugs and a significant obstacle to formal verification. When many parts of a program can access and modify the same object, it becomes difficult to reason about what state the object is in at any given point.

**Unique ownership** is a type-level discipline that addresses this: a `@Unique` reference has exactly one owner at any time. Ownership can be transferred, but it cannot be duplicated. This makes it safe to reason about the object's state locally — no other part of the program can mutate it concurrently.

SnaKt's uniqueness checker enforces these rules at the Kotlin source level, before verification, to catch ownership violations early with friendly Kotlin-level error messages.

---

## The `@Unique` annotation

A value annotated `@Unique` has exclusive ownership. This means:

- Only one variable can hold the value at a time.
- Passing a `@Unique` value to a function parameter annotated `@Unique` **transfers ownership** to the callee — the caller no longer holds the reference after the call.
- Returning a `@Unique` value transfers ownership to the caller.
- Assigning a `@Unique` value to a new variable moves it — the original variable is no longer usable.

```kotlin
@Unique val obj = createObject()  // obj has unique ownership
useUnique(obj)                     // ownership transferred to useUnique
// obj is no longer accessible here
```

---

## The `@Borrowed` annotation

`@Borrowed` is an annotation on parameters that allows a function to accept a `@Unique` value **without consuming it**. The caller retains ownership after the call:

```kotlin
fun inspect(@Borrowed obj: MyClass) {
    // can read obj's fields, but cannot transfer ownership
}

@Unique val obj = createObject()
inspect(obj)   // obj is borrowed; ownership not transferred
// obj is still accessible here
```

`@Borrowed` is the "lending" mechanism: the callee gets temporary access to a unique value, and ownership returns to the caller when the function returns.

---

## The uniqueness checker

The uniqueness checker is implemented as `UniqueDeclarationChecker`, a standalone `FirDeclarationChecker<FirSimpleFunction>`. It runs independently of the Viper verification pass and reports errors directly as Kotlin diagnostics.

The checker operates on the FIR AST, walking the function body and tracking which unique values are currently owned by which variables.

### Error cases

| Situation | Error |
|---|---|
| Assigning a unique value to two variables | "Value has already been moved" |
| Using a unique variable after it was transferred | "Use of moved value" |
| Passing a `@Borrowed` value to a `@Unique` (consuming) parameter | "Cannot pass borrowed value to consuming parameter" |
| Returning a `@Borrowed` parameter | "Cannot return a borrowed value" |
| Storing a unique value in a shared (non-unique) field | "Cannot store unique value in shared field" |

---

## `ContextTrie`

The uniqueness checker tracks ownership paths using a **`ContextTrie`**: a trie (prefix tree) data structure that represents the ownership state of all currently-accessible unique values.

### Why a trie?

Ownership in Kotlin is not limited to simple variables — it can extend through field chains:

```kotlin
val container = Container()
container.field = createUniqueValue()  // container.field is unique
```

The trie represents these paths: `container` is a node with a child `field`. The leaf nodes carry the ownership status (owned, moved, borrowed).

### Trie operations

| Operation | Meaning |
|---|---|
| `consume(path)` | Mark a unique path as consumed (moved); error if already consumed |
| `borrow(path)` | Mark a path as borrowed for the duration of a call |
| `restore(path)` | Restore ownership after a `@Borrowed` call returns |
| `merge(trie1, trie2)` | Merge ownership states from two branches; error if they disagree |
| `fork()` | Clone the current trie for exploration of a branch |

### Branch merging

When the uniqueness checker encounters an `if`/`else`, it forks the trie, processes each branch independently, then merges the resulting tries. If a unique value is consumed in one branch but not the other, this is an error: after the `if`/`else`, it would be ambiguous whether ownership has been transferred.

---

## Interaction with Viper verification

The uniqueness checker enforces ownership rules at the Kotlin AST level, but ownership also affects how Viper verifies the code:

### Exclusive predicates

Fields holding `@Unique` values use the **unique predicate** instead of the shared predicate (see [Permissions](permissions.md)). The unique predicate bundles `write` (full, exclusive) permissions rather than `rd` (shared read) permissions. This allows Viper to verify that unique fields are not concurrently accessible.

### Method pre/postconditions

Ownership transfer is encoded in Viper method contracts:

- A method accepting a `@Unique` parameter has a precondition that the caller holds `write` permission for the unique predicate.
- A method returning a `@Unique` value has a postcondition that the caller gains `write` permission.
- A method with a `@Borrowed` parameter takes `write` permission and returns it: the permission appears in both the precondition and the postcondition, modeling the loan.

This means that the Viper verifier independently confirms the ownership discipline established by the uniqueness checker, providing an additional layer of soundness.

---

## Summary of the two-layer approach

| Layer | Tool | What it checks |
|---|---|---|
| Kotlin AST level | `UniqueDeclarationChecker` + `ContextTrie` | Ownership flows in source code: no double-consume, no use-after-move, no borrow violations |
| Viper level | Silicon + Z3 | Permission accounting in the heap model: exclusive predicates, correct pre/postconditions for ownership transfer |

The two layers are complementary. The Kotlin-level check gives fast, friendly error messages during development. The Viper-level check provides a mathematical proof that the ownership discipline is respected in all possible executions.
