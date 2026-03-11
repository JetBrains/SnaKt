# SSA Transformation

This page describes how SnaKt applies Static Single Assignment (SSA) form during the linearization of Kotlin function bodies to Viper.

Source: `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/linearization/SsaConverter.kt`

---

## What is SSA?

**Static Single Assignment** is a program representation in which every variable is assigned exactly once. Instead of reassigning an existing variable, each new assignment creates a fresh version of that variable. For example:

```
x = 1       becomes     x$1 = 1
x = x + 1              x$2 = x$1 + 1
```

SSA is widely used in compilers and formal verification tools because:

- It makes data flow explicit: each use of a variable names exactly one definition.
- SMT solvers reason more efficiently over single-assignment form because there is no aliasing between different uses of the same name.
- It eliminates the need for phi-nodes to track which assignment reached a given program point — instead, a join at a merge point produces a new version that is a conditional expression over the predecessor versions.

---

## Why SnaKt needs SSA

Viper is an intermediate verification language with a functional-style logic for specifications. Viper `function`s (the pure fragment, used in contracts and quantifiers) must be represented as side-effect-free expressions — they cannot contain imperative assignment statements.

When SnaKt translates a `@Pure` Kotlin function to a Viper `function`, the entire body must be a single Viper expression. This means that variables mutated by the Kotlin code must be unrolled into a chain of `let`-bindings:

```viper
let x$1 == (1) in
let x$2 == (x$1 + 1) in
x$2
```

For impure functions translated to Viper `method`s, SSA-style naming is still beneficial because it avoids variable name collisions across branches and makes the verification conditions cleaner for Z3.

---

## How SnaKt implements SSA

### Variable versioning

Each write to a Kotlin local variable creates a fresh version name. The naming convention appends a version counter separated by `$`:

```
x → x$1 → x$2 → x$3
```

These names are produced by `FreshEntityProducer<SsaVariableName, SymbolicName>` inside `SsaConverter`. Each source variable name has its own producer, so versions are per-variable:

- `x$1`, `x$2`, `x$3` — versions of `x`
- `y$1`, `y$2` — versions of `y`

### Branching and merging

When the control flow splits at an `if`/`else`, `SsaConverter.branch(condition, thenBlock, elseBlock)` is called. It:

1. Saves the current SSA state as the split point.
2. Processes the `then` branch, producing a new head state.
3. Processes the `else` branch from the same split point.
4. Creates an `SsaJoinNode` that, for each variable written in either branch, produces a new version whose value is a conditional expression: `condition ? thenVersion : elseVersion`.

This is equivalent to a phi-node in traditional SSA, but expressed as a Viper ternary expression (or a Viper `if(condition) { x$3 := thenVersion } else { x$3 := elseVersion }` in the method body).

### `SsaConverter` state

`SsaConverter` maintains:

- `head: SsaBlockNode` — the current block node, tracking the path condition under which the current code executes.
- `ssaAssignments` — the list of `let`-bindings accumulated so far (for pure functions).
- `returnExpressions` — the list of conditional return values (for pure functions with multiple return paths).
- `ssaNameProducers` — one `FreshEntityProducer` per source variable, for generating fresh SSA names.

For `@Pure` functions, `SsaConverter.constructExpression()` assembles the entire body into a single nested Viper expression:

```
ssaAssignments.foldRight(finalBody) { (name, value), inner ->
    LetBinding(name, value, inner)
}
```

with multiple return paths merged via nested ternary expressions.

---

## Example

Consider this Kotlin function:

```kotlin
@Pure
fun clamp(x: Int, lo: Int, hi: Int): Int {
    var result = x
    if (result < lo) result = lo
    if (result > hi) result = hi
    return result
}
```

After SSA conversion, the embedding becomes equivalent to:

```viper
function clamp(x: Ref, lo: Ref, hi: Ref): Ref
{
  let result$1 == (x) in
  let result$2 == (result$1 < lo ? lo : result$1) in
  let result$3 == (result$2 > hi ? hi : result$2) in
  result$3
}
```

Each mutation of `result` creates a new SSA version. No variable is ever assigned twice. Z3 can reason about this expression directly as a mathematical formula.

---

## `SharedLinearizationState`

For impure functions (translated to Viper `method`s), SSA-style fresh variables are managed by `SharedLinearizationState`:

```kotlin
class SharedLinearizationState(
    private val producer: FreshEntityProducer<AnonymousVariableEmbedding, TypeEmbedding>
) {
    fun freshAnonVar(type: TypeEmbedding) = producer.getFresh(type)
}
```

`SharedLinearizationState` is shared across the entire linearization of a single function body. It provides `freshAnonVar`, which produces a new `AnonymousVariableEmbedding` with a unique name each time it is called. These anonymous variables hold intermediate results that cannot be inlined into a single Viper expression.

The combination of `SsaConverter` (for pure functions) and `SharedLinearizationState` (for impure functions) ensures that no two variables in the generated Viper code share the same name within a function body.
