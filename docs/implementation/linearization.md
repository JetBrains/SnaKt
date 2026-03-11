# Linearization

Linearization is the stage that takes a tree of `ExpEmbedding` nodes produced by the conversion engine and flattens it into a sequence of Viper statements (`Stmt.Seqn`). The result is then transformed into SSA form before being handed to Silicon for verification.

The linearization code lives in `formver.compiler-plugin/core/src/…/core/linearization/`.

---

## What linearization does

After conversion, a Kotlin function body is represented as a single nested `ExpEmbedding` tree. For example, a function call with two sub-expressions is a single tree node containing its argument sub-trees. Viper, however, requires a flat sequence of statements. Linearization:

1. Walks the `ExpEmbedding` tree recursively.
2. Emits Viper statements for any side-effecting operations (assignments, method calls, control flow).
3. Returns a `Stmt.Seqn` (statement sequence) that can be used as the body of a Viper method.

For `@Pure` functions, whose bodies must be single Viper expressions rather than statement sequences, a separate `PureLinearizer` is used.

---

## The three lowering methods

Every `ExpEmbedding` subtype implements three lowering methods on the `LinearizationContext` interface:

| Method | Context | Result |
|---|---|---|
| `toViperUnusedResult(ctx)` | Statement context — the result value is discarded | Emits Viper statements; no return value needed |
| `toViperStoringIn(variable, ctx)` | Store result in `variable` | Emits Viper statements; the result is assigned to `variable` |
| `toViperBuiltinType(ctx)` | Pure built-in expression (inside spec or condition) | Returns a `Viper.Exp` directly without emitting statements |

These three methods allow the linearizer to handle the same embedding node differently depending on whether its value is needed and whether it appears in a pure or impure context.

---

## Linearizer

`Linearizer` is a Kotlin `data class` implementing `LinearizationContext`. It is the main linearization context for impure (non-`@Pure`) functions.

### Key fields

| Field | Type | Purpose |
|---|---|---|
| `state` | `SharedLinearizationState` | Fresh name source, shared across the whole function body |
| `seqnBuilder` | `SeqnBuilder` | Accumulates Viper statements for the current scope |
| `source` | `KtSourceElement?` | Current source position for Viper position annotations |
| `stmtModifierTracker` | `StmtModifierTracker?` | Tracks statement modifiers (fold/unfold) applied around a single statement |

### Policies

The `Linearizer` applies two policies that differ from `PureLinearizer`:

- `UnfoldPolicy.UNFOLD`: predicates are unfolded using Viper `unfold`/`fold` statements around the access.
- `LogicOperatorPolicy.CONVERT_TO_IF`: boolean `&&`/`||` in the Kotlin source become Viper `if` branches, enabling short-circuit evaluation.

### Key operations

- `freshAnonVar(type)`: produces a fresh `AnonymousVariableEmbedding` and immediately emits a `LocalVarDecl` for it in the current `SeqnBuilder`.
- `asBlock(action)`: creates a fresh `SeqnBuilder`, runs `action` in a sub-linearizer that uses it, and returns the resulting `Stmt.Seqn`. Used to produce the then/else branches of `Stmt.If` nodes.
- `store(lhs, rhs)`: emits a `Stmt.LocalVarAssign` or `Stmt.FieldAssign` depending on `lhs`.
- `addReturn(returnExp, target)`: stores `returnExp` into the return target variable and emits a `Stmt.Goto` to the return label.
- `addBranch(condition, thenBranch, elseBranch, type, result)`: emits a `Stmt.If`; each branch is linearized into a fresh `Seqn` via `asBlock`.

---

## SeqnBuilder

`SeqnBuilder` accumulates Viper statements and variable declarations into a `Stmt.Seqn` node.

It holds two mutable lists:
- `statements: MutableList<Stmt>` — the ordered sequence of Viper statements.
- `declarations: MutableList<Declaration>` — the scoped local variable and label declarations.

The `block` property returns the `Stmt.Seqn` node backed by these lists. Because `Stmt.Seqn` is a data class that holds references to the same underlying lists, statements added after the `block` property is first accessed are still reflected in the node.

Nested blocks are produced by creating a new `SeqnBuilder` (via `Linearizer.asBlock`) and letting the inner linearizer append to it. This naturally produces nested `Stmt.Seqn` nodes in the Viper AST, which is the correct structure for block-scoped variables in Viper.

---

## SsaConverter

`SsaConverter` transforms the linearized statement sequence into Static Single Assignment (SSA) form. In SSA, every variable is assigned exactly once; subsequent writes to the same variable create a new version of the variable.

### Why SSA?

Silicon (the Viper verifier) works most efficiently when each variable is written once. SSA eliminates the need for Silicon to track multiple versions of the same variable internally, and makes it easier to reason about value flow in the presence of conditionals.

### How it works

`SsaConverter` maintains:

- `head: SsaBlockNode` — the current position in the SSA graph.
- `ssaAssignments: MutableList<Pair<SsaVariableName, Exp>>` — the accumulated SSA let-bindings.
- `returnExpressions: MutableList<Pair<Exp, Exp>>` — pairs of (path condition, return expression).
- `ssaNameProducers: MutableMap<SymbolicName, FreshEntityProducer<SsaVariableName, SymbolicName>>` — per-variable fresh name generators.

Key operations:

- `branch(condition, thenBlock, elseBlock)`: splits the SSA graph at the current point, runs both branches, then merges them at a `SsaJoinNode` which emits phi-assignment let-bindings.
- `addAssignment(name, varExp)`: assigns a new SSA name to `name` at the current head node.
- `addReturn(returnExp)`: records this return expression with its full path condition.
- `constructExpression()`: folds all SSA assignments and return expressions into a single nested Viper `let`/ternary expression that represents the whole function's result.

For impure functions, the SSA converter is used through `PureLinearizer` rather than `Linearizer` (the impure path uses regular Viper assignments). The SSA conversion for pure functions produces a single Viper `Exp` containing nested `let` bindings.

---

## SsaNode and SsaPhiMerge

The SSA graph is represented by a linked structure of `SsaNode` subtypes:

- `SsaStartNode`: the entry point of the SSA graph.
- `SsaBlockNode`: a node in a linear sequence of assignments. Holds its predecessor node and a branching condition (which path condition leads to this node).
- `SsaJoinNode`: created at the merge point of two branches. Emits phi-assignments (ternary `let` bindings) for variables that were written in both branches.

---

## SharedLinearizationState

`SharedLinearizationState` holds the state that must be shared across the entire function body during linearization — currently just the `FreshEntityProducer` for anonymous variables. It is passed to every `Linearizer` and `PureLinearizer` instance created during a single function's linearization.

This ensures that all anonymous variables generated throughout the function (including those inside nested blocks and branches) have globally unique names.

---

## PureLinearizer

`PureLinearizer` is the linearization context for pure expressions — those that appear inside specifications (preconditions, postconditions, loop invariants) and `@Pure` function bodies.

### Key differences from `Linearizer`

| Aspect | `Linearizer` | `PureLinearizer` |
|---|---|---|
| `asBlock` | Creates a nested `Seqn` | Throws `PureLinearizerMisuseException` |
| `addStatement` | Emits a Viper statement | Throws `PureLinearizerMisuseException` |
| `store` | Emits `Stmt.LocalVarAssign` | Records SSA assignment via `SsaConverter` |
| `addReturn` | Emits goto | Records return expression via `SsaConverter` |
| `UnfoldPolicy` | `UNFOLD` (statement unfold/fold) | `UNFOLDING_IN` (Viper `unfolding … in …` expression) |
| `LogicOperatorPolicy` | `CONVERT_TO_IF` | `CONVERT_TO_EXPRESSION` (uses Viper `&&`/`\|\|` directly) |

### Result construction

After all sub-expressions have been linearized, `constructExpression()` delegates to `SsaConverter.constructExpression()` to fold the recorded SSA assignments and return expressions into a single Viper `Exp`.

The helper function `ExpEmbedding.pureToViper(toBuiltin, source)` wraps this process and converts `PureLinearizerMisuseException` into a descriptive `IllegalStateException` that includes the debug view of the offending embedding.

---

## Key source files

| File | Role |
|---|---|
| `Linearizer.kt` | Main linearization context for impure functions |
| `PureLinearizer.kt` | Linearization context for pure expressions and `@Pure` functions |
| `SeqnBuilder.kt` | Accumulates Viper statements into a `Stmt.Seqn` |
| `SsaConverter.kt` | Transforms assignments into SSA form |
| `SharedLinearizationState.kt` | Shared fresh-variable source across a function body |
| `LinearizationContext.kt` | Interface defining the lowering methods |
