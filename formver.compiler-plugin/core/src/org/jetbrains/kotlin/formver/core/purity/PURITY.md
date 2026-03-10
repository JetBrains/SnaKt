# Purity Checks

This package implements SnaKt's purity analysis: the system that verifies whether a
function annotated with `@Pure` actually contains only pure (side-effect-free) expressions,
and that detects validity violations in any embedding tree.

---

## Motivation

In Viper, **functions** (pure) and **methods** (impure) are distinct concepts.
A Viper `function` may appear inside specifications (pre/postconditions, loop invariants,
`forall` bodies), while a Viper `method` may not.
SnaKt therefore requires that any Kotlin function annotated with `@Pure` compiles entirely
to pure expressions so it can be emitted as a Viper `function`.

The purity package provides two complementary checks:

| Check | What it tests | Entry point | Scope |
|---|---|---|---|
| **Purity check** | Is every node in this embedding a pure expression? | `ExpEmbedding.isPure()` | `@Pure` functions only |
| **Validity check** | Does each node satisfy its own structural invariants? | `ExpEmbedding.checkValidity(...)` | All function bodies |

These are **independent concerns**. The validity check is not about whether a function is
`@Pure` — it checks node-level structural constraints that must hold in any embedding,
pure or impure. The name "purity" in the package refers to the broader notion of
"things that must hold about expression embeddings".

Currently the only structural constraint enforced by `isValid` is: **`Assert` conditions
must be pure expressions** (since Viper `assert` requires a pure boolean argument).
All other node types inherit the default `isValid` which unconditionally returns `true`.
The infrastructure exists to add further per-node constraints in the future.

---

## Files

### `PurityContext.kt`

A single-method interface:

```kotlin
interface PurityContext {
    fun addPurityError(embedding: ExpEmbedding, msg: String)
}
```

Abstracts over error reporting during validity checks. Each node's `isValid` method receives
a `PurityContext` and calls `addPurityError` for every violation it finds, rather than
throwing immediately — this ensures all errors in a tree are collected before reporting.

---

### `DefaultPurityContext.kt`

The standard implementation of `PurityContext` used during compilation.
Resolves the best available source position for the offending embedding (via
`expressionSource`) and forwards the error to the shared `ErrorCollector`, which
later emits it as a `PURITY_VIOLATION` diagnostic.

---

### `ExprUtils.kt`

Utility functions that operate on an `ExpEmbedding` tree:

#### `preorder(currentSource)`
Produces a pre-order sequence of `(ExpEmbedding, KtSourceElement?)` pairs across the
entire tree. Source positions are inherited downwards from the nearest enclosing
`WithPosition` node, since most internal nodes carry no position of their own.
`WithPosition` nodes themselves are skipped in the output (they are transparent wrappers).

#### `checkValidity(source, errorCollector)`
Walks the tree in pre-order and calls `embedding.isValid(purityContext)` on every node.
Uses `exhaustiveAll` instead of `all` to **avoid short-circuiting** — every node is
visited even after the first failure, so all violations are reported together.

#### `isPure()`
Runs `ExprPurityVisitor` on the root of the embedding tree.
Returns `true` only if the visitor returns `true` for every node.
Used in `StmtConversionContext.convertFunctionWithBody` to validate `@Pure` function bodies
before handing them to `PureLinearizer`.

#### `expressionSource(fallback)` *(internal)*
Finds the source position of an expression by walking up through transparent
`SharingContext` wrappers to the first `WithPosition` node. Returns `fallback`
if no position is found. Used by `DefaultPurityContext` to produce accurate
diagnostic locations.

---

### `ExpPurityVisitor.kt`

A `ExpVisitor<Boolean>` that classifies every node in an embedding tree as pure or impure.
Instantiated fresh for each `isPure()` call; carries a `declaredVariables` set to track
which variables have been declared within the current scope (needed for assignment purity).

#### Pure nodes (return `true`)

| Node | Reason |
|---|---|
| `UnitLit`, `LiteralEmbedding`, `BooleanLit`, etc. | Constants; no heap access |
| `VariableEmbedding` | Read of a local variable; no heap access |
| `FunctionCall` | Viper `function` call — by definition pure in Viper |
| `ExpWrapper` | Transparent wrapper; delegates purity to its content |
| `Declare` (with initializer) | Declares a local variable; the variable is added to `declaredVariables` |
| `Assign` (to declared variable) | Assignment to a locally declared variable only |

#### Structurally delegating nodes (pure iff all children are pure)

`Block`, `Return`, `If`, `BinaryOperatorExpEmbedding`, `UnaryOperatorExpEmbedding`,
`SequentialAnd`, `SequentialOr`, `EqCmp`, `NeCmp`, `WithPosition`,
`InjectionBasedExpEmbedding`, `SharingContext`

These nodes have no side effects of their own; their purity depends entirely on their
sub-expressions, checked via `allChildrenPure(visitor)`.

#### Unconditionally impure nodes (return `false`)

| Node | Reason |
|---|---|
| `MethodCall` | Viper `method` call — inherently impure (TODO: whitelist for `@Pure`-annotated methods) |
| `FieldAccess`, `PrimitiveFieldAccess` | Heap read — requires permissions (TODO: may be relaxable) |
| `FieldModification` | Heap write |
| `Assert` | Produces a Viper `assert` statement |
| `While` | Loop — not an expression |
| `Goto`, `GotoChainNode` | Control-flow jump |
| `LambdaExp`, `FunctionExp`, `InvokeFunctionObject` | Higher-order / impure call structures |
| `Shared`, `InhaleDirect`, `InhaleInvariants` | Permission operations |
| `FieldAccessPermissions`, `PredicateAccessPermissions`, `AccEmbedding` | Permission assertions |
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

`isPure()` is a fast pre-flight check that gates entry to `PureLinearizer`.
`checkValidity()` is a deeper structural check run on all function bodies (pure and impure)
that catches violations that `isPure()` does not — for example, nodes that are individually
pure but structurally malformed.

### The role of `checkValidity` in purity

`checkValidity` is **not** a purity check for `@Pure` functions and should not be
confused with one. Its purpose is different:

- `isPure()` answers: *"Is this embedding a legal body for a Viper `function`?"*
  It classifies every node as pure or impure using `ExprPurityVisitor` and is only
  called for `@Pure`-annotated functions.

- `checkValidity()` answers: *"Does each node satisfy its own local structural
  invariant, regardless of the function's purity?"*
  It runs on **all** function bodies via `isValid()` on each node. The default
  `isValid` implementation on `ExpEmbedding` unconditionally returns `true`, so
  only nodes that explicitly override it participate in this check.

**Currently, the only node that overrides `isValid` is `Assert`**, which requires
its condition to be a pure expression (because Viper's `assert` statement only
accepts a side-effect-free boolean). All other nodes have no additional structural
constraints and pass validation by default.

The `PurityContext` / `DefaultPurityContext` infrastructure, and the `PURITY_VIOLATION`
diagnostic, therefore exist primarily to support this `Assert`-condition check today,
with the design allowing further per-node constraints to be added in future without
changing the traversal machinery.
