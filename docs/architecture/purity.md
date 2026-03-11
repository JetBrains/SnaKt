# Purity

This page describes SnaKt's purity analysis: the system that verifies whether a function annotated with `@Pure` contains only pure (side-effect-free) expressions, and that detects structural validity violations in any embedding tree.

Source: `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/purity/`

---

## Motivation

In Viper, **functions** (pure) and **methods** (impure) are fundamentally distinct concepts:

- A Viper `function` may appear inside specifications: preconditions, postconditions, loop invariants, and `forall` bodies. It must be free of side effects.
- A Viper `method` may not appear in specifications. It may read and write heap state, call other methods, and produce non-deterministic results.

SnaKt maps `@Pure`-annotated Kotlin functions to Viper `function`s and all others to Viper `method`s. This means that only `@Pure` functions can be called within `preconditions {}`, `postconditions {}`, or `loopInvariants {}` blocks.

---

## Two complementary checks

| Check | Question it answers | Entry point | Applies to |
|---|---|---|---|
| **Purity check** | Is every node in this embedding tree a legal pure expression? | `ExpEmbedding.isPure()` | `@Pure` functions only |
| **Validity check** | Does each node satisfy its own structural invariant? | `ExpEmbedding.checkValidity(...)` | All function bodies |

These are independent concerns. The purity check gates translation to a Viper `function`. The validity check enforces per-node structural constraints in all embedding trees, regardless of purity.

---

## `PurityContext` and `DefaultPurityContext`

```kotlin
interface PurityContext {
    fun addPurityError(embedding: ExpEmbedding, msg: String)
}
```

`PurityContext` abstracts over error reporting during validity checks. Each node's `isValid` method receives a `PurityContext` and calls `addPurityError` for each violation it finds. Errors are accumulated rather than thrown immediately, so all violations in a tree are collected in one pass.

`DefaultPurityContext` is the standard implementation used during compilation. It resolves the best available source position for the offending embedding (via `expressionSource`) and forwards the error to `ErrorCollector`, which emits a `PURITY_VIOLATION` diagnostic.

---

## `ExprPurityVisitor`

`ExprPurityVisitor` is an `ExpVisitor<Boolean>` that classifies every node in an embedding tree as pure or impure. It is instantiated fresh for each `isPure()` call and carries a `declaredVariables` set to track variables declared within the current scope (needed for assignment purity).

### Pure nodes

| Node | Reason |
|---|---|
| `UnitLit`, `LiteralEmbedding` subtypes | Constants; no heap access |
| `VariableEmbedding` | Read of a local variable; no heap access |
| `FunctionCall` | A Viper `function` call — by definition pure in Viper |
| `ExpWrapper` | Transparent wrapper; delegates purity to its content |
| `Declare` (with initializer) | Declares a local variable; adds it to `declaredVariables` |
| `Assign` (to a locally declared variable) | Assignment to a variable declared within the current scope only |

### Structurally delegating nodes

These nodes are pure if and only if all their child nodes are pure. They have no side effects of their own.

`Block`, `Return`, `If`, `BinaryOperatorExpEmbedding`, `UnaryOperatorExpEmbedding`, `SequentialAnd`, `SequentialOr`, `EqCmp`, `NeCmp`, `WithPosition`, `InjectionBasedExpEmbedding`, `SharingContext`

### Unconditionally impure nodes

| Node | Reason |
|---|---|
| `MethodCall` | Viper `method` call — inherently impure |
| `FieldAccess`, `PrimitiveFieldAccess` | Heap read — requires permissions |
| `FieldModification` | Heap write |
| `Assert` | Produces a Viper `assert` statement |
| `While` | Loop — not an expression |
| `Goto`, `GotoChainNode` | Control-flow jump |
| `LambdaExp`, `FunctionExp`, `InvokeFunctionObject` | Higher-order or impure call structures |
| `Shared`, `InhaleDirect`, `InhaleInvariants` | Permission operations |
| `FieldAccessPermissions`, `PredicateAccessPermissions` | Permission assertions |
| `NonDeterministically` | Non-deterministic choice |
| `LabelExp` | Label declaration |
| `Elvis`, `SafeCast`, `Cast`, `Is` | Currently unsupported in pure contexts |
| `Old`, `ForAllEmbedding` | Specification-only; not valid as function body expressions |
| `ErrorExp` | Error sentinel |

---

## How the two checks relate

```
@Pure fun f(...): T { body }
         │
         ▼
  StmtConversionContext.convertFunctionWithBody()
         │
         ├─ convert(firBody) ──────────────────► ExpEmbedding tree
         │
         ├─ isPure()          ──► ExprPurityVisitor
         │    └─ false? ──► SnaktInternalException (INTERNAL_ERROR)
         │
         ├─ PureLinearizer.toViperUnusedResult()   (produces Viper Exp)
         │
         └─ (separately, for all functions)
            checkValidity()  ──► preorder() + isValid() per node
                 └─ violations ──► DefaultPurityContext ──► ErrorCollector
                                        └─ PURITY_VIOLATION diagnostic
```

`isPure()` is a fast pre-flight check that gates entry to `PureLinearizer`. If a function is annotated `@Pure` but its body contains impure nodes, an `INTERNAL_ERROR` is raised (this indicates the conversion produced an embedding that should not have passed earlier type-checking).

`checkValidity()` is a deeper structural check run on **all** function bodies (pure and impure). It catches violations that `isPure()` does not — for example, nodes that are individually acceptable but structurally malformed.

### The role of `checkValidity`

`checkValidity` is **not** a purity check for `@Pure` functions. Its purpose is different:

- `isPure()` answers: *"Is this embedding a legal body for a Viper `function`?"*
- `checkValidity()` answers: *"Does each node satisfy its own local structural invariant, regardless of the function's purity?"*

Currently, the **only** node that overrides `isValid` is `Assert`, which requires its condition to be a pure expression because Viper's `assert` statement only accepts a side-effect-free boolean. All other nodes have no additional structural constraints and pass validation by default.

The `PurityContext` / `DefaultPurityContext` infrastructure and the `PURITY_VIOLATION` diagnostic therefore exist today primarily to support this `Assert`-condition check, with the design ready for further per-node constraints to be added without changing the traversal machinery.

---

## `ExprUtils` helpers

`preorder(currentSource)` produces a pre-order sequence of `(ExpEmbedding, KtSourceElement?)` pairs across the entire tree. Source positions are inherited downward from the nearest enclosing `WithPosition` node, since most internal nodes carry no position of their own. `WithPosition` nodes themselves are transparent in the sequence.

`checkValidity(source, errorCollector)` walks the tree in pre-order and calls `embedding.isValid(purityContext)` on every node, using `exhaustiveAll` instead of `all` to avoid short-circuiting — every node is visited even after the first failure, so all violations are reported together in one compilation.
