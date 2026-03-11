# Conversion Engine

The conversion engine lives in `formver.compiler-plugin/core/src/…/core/conversion/` and is responsible for translating Kotlin's Frontend Intermediate Representation (FIR) into the intermediate `ExpEmbedding` tree that will later be linearized into Viper.

---

## The three-context hierarchy

Conversion is organised as a three-level hierarchy of contexts. Each level extends the one above it with the additional state needed at that scope.

```
ProgramConversionContext
    └── MethodConversionContext
            └── StmtConversionContext
```

### ProgramConversionContext

`ProgramConversionContext` (implemented by `ProgramConverter`) is the top-level context for an entire Viper program derived from a single Kotlin function under analysis. It:

- Owns the registries of all deduplicated embeddings: methods, pure functions, classes, properties, fields.
- Holds all `FreshEntityProducer` instances that must be unique across the whole program (while-loop indices, catch labels, try-exit labels, scope indices, anonymous variables, return targets).
- Provides the `embedType`, `embedFunction`, `embedPureFunction`, `embedProperty` entry points used by lower levels.
- Assembles the final `Program` AST after all functions have been registered.

### MethodConversionContext

`MethodConversionContext` (implemented by `MethodConverter`) is the function-level context. It:

- Owns a `PropertyResolver` tracking local variables and loop labels for the current scope chain.
- Holds a `ParameterResolver` for formal parameters and receivers of the current function.
- Has an optional `parent` reference to the enclosing `MethodConversionContext` for lambda captures: resolution first tries the current scope, then walks up to `parent`.
- Delegates all program-level operations (type embedding, fresh variable production, etc.) to its `ProgramConversionContext`.

The parent chain models lexical scoping: a lambda's converter has the outer function's converter as its parent, so free variables in the lambda body resolve through the chain. An inlined function does **not** set the caller as its parent, because inlined functions cannot access the caller's local variables.

### StmtConversionContext

`StmtConversionContext` (interface, implemented by `StmtConverter`) is the statement-level context. It extends `MethodConversionContext` with mutable state needed while walking a FIR function body:

- `whenSubject`: the temporary variable holding the `when` expression subject.
- `checkedSafeCallSubject`: the pre-evaluated receiver of the current safe-call `?.` expression.
- `activeCatchLabels`: the stack of active exception handler entry labels.

`StmtConverter` is a Kotlin `data class` and is therefore immutable. Every `withX { }` helper (`withWhenSubject`, `withFreshWhile`, `withCatches`, etc.) produces a fresh copy of the converter for the duration of the block, then discards it. This makes the conversion code easy to reason about: the outer converter is always unchanged after a nested conversion.

---

## ProgramConverter

`ProgramConverter` is the central conversion context. It is instantiated once per function analysis by `ViperPoweredDeclarationChecker` and is the single source of truth for the Viper `Program` handed to Silicon.

### Embedding registries

| Registry | Type | Contains |
|---|---|---|
| `methods` | `MutableMap<SymbolicName, FunctionEmbedding>` | Viper impure methods: pre-populated with special stdlib functions and partially-special functions; user functions added on demand. |
| `functions` | `MutableMap<SymbolicName, PureFunctionEmbedding>` | Viper pure functions: user `@Pure` functions added via `embedPureUserFunction`. |
| `classes` | `MutableMap<SymbolicName, ClassTypeEmbedding>` | All class types seen; used to build `RuntimeTypeDomain`. |
| `properties` | `MutableMap<SymbolicName, PropertyEmbedding>` | Getter + optional setter for all properties referenced in the body. |
| `fields` | `MutableSet<FieldEmbedding>` | Viper fields collected from all processed class backing fields. |

Calling `embedFunction` or `embedClass` twice for the same symbol always returns the same instance. This deduplication ensures the generated Viper program contains each declaration exactly once.

### Registering a function for verification

The entry point is `registerForVerification(declaration: FirSimpleFunction)`:

1. Build the full function signature (parameter types, pre/postconditions, symbolic name) via `embedFullSignature`.
2. Create a `MethodConverter` and its `StmtConversionContext` for body conversion.
3. Route to the pure or impure path based on `@Pure`:
   - `@Pure` → `embedPureUserFunction` + `convertFunctionWithBody` → produces a Viper `function` with an expression body.
   - non-pure → `embedUserFunction` + `convertMethodWithBody` → produces a Viper `method` with a statement body.

The embedding is registered in the map **before** the body is assigned. This allows mutually recursive functions to find each other's signatures without an infinite loop.

### Class embedding

Class embedding proceeds in four phases to avoid circular dependencies (a class may reference itself through its own fields):

1. Register a name-only placeholder in `classes` to break cycles.
2. Initialise details (`ClassEmbeddingDetails`) and supertypes (recursively embedding each supertype).
3. Initialise fields from the class's property backing fields.
4. Process property embeddings (getter/setter) for each property.

### Program assembly

The `program` property assembles the complete Viper `Program` from all registered embeddings:

- `domains`: the runtime-type domain derived from all embedded classes.
- `fields`: special built-in fields plus user-defined backing fields (deduplicated by name).
- `functions`: built-in pure functions plus user `@Pure` functions (deduplicated).
- `methods`: built-in methods plus user non-pure functions (deduplicated).
- `predicates`: shared/unique access predicates for every embedded class.

---

## MethodConverter

`MethodConverter` owns:

- A `PropertyResolver` for local variables and loop labels within this scope.
- A `ParameterResolver` for formal parameters and receivers of this function.
- An optional `parent` for symbol lookup in lexically enclosing scopes.

Key operations:

- `resolveLocal(symbol)`: searches the current scope's `PropertyResolver`, then walks to `parent`.
- `resolveParameter(symbol)`: tries `paramResolver`, then `parent`.
- `registerLocalProperty(symbol)`: registers a `val`/`var` in the current scope.
- `withScopeImpl(scopeDepth, action)`: enters a new inner scope for the duration of `action`.
- `retrievePropertiesAndParameters()`: yields all visible `VariableEmbedding`s (used when building the Viper method's local variable declaration list).

---

## StmtConverter and StmtConversionVisitor

`StmtConverter.convert(stmt: FirStatement)` is the single entry point for all FIR-to-embedding conversion within a function body. It delegates to `StmtConversionVisitor`, which dispatches on the FIR node type using the visitor pattern.

`StmtConversionVisitor` handles:

- Blocks, return statements, local variable declarations.
- `if`, `when`, `while`, `for` expressions.
- Safe-call (`?.`) and Elvis (`?:`) operators.
- Function calls (checking the special-functions registry first).
- Property accesses (backing field or custom getter/setter).
- Try-catch, throw, labels, and `break`/`continue`.
- Smart-cast type checks.

The result of every conversion is an `ExpEmbedding` node with an attached source position (from `stmt.source`).

---

## ContractDescriptionConversionVisitor

`ContractDescriptionConversionVisitor` translates Kotlin `contract { }` blocks into pre/postcondition `ExpEmbedding`s that are attached to the function's `FullNamedFunctionSignature`. It handles:

- `returns()` / `returns(true)` / `returns(false)` / `returns(null)` / `returnsNotNull()` effects.
- `callsInPlace` effects (translated to inlining information).
- Condition expressions referencing function parameters.

---

## AccessPolicy

`AccessPolicy` is an enum that determines the permission level for a field access in the generated Viper code:

| Value | Meaning |
|---|---|
| `ALWAYS_INHALE_EXHALE` | Permission is inhaled before access and exhaled after; used for fields that are accessed but whose ownership is not tracked. |
| `ALWAYS_READABLE` | Read-only access; the field can be read but not written through this embedding. |
| `ALWAYS_WRITEABLE` | Full write permission; the field can be both read and written. |
| `MANUAL` | Permission management is handled manually (e.g. via `@Manual` annotations). |

---

## FreshEntityProducer

`FreshEntityProducer<R, S>` is a stateful factory that produces fresh instances of type `R` by supplying a monotonically increasing integer index together with a caller-provided seed value of type `S`. The index guarantees uniqueness across all entities produced by the same instance.

Used throughout conversion and linearization to generate:

- Anonymous Viper variables (`AnonymousVariableEmbedding`, `AnonymousBuiltinVariableEmbedding`).
- Return target variables (`ReturnTarget`).
- While-loop indices.
- Catch label names (`CatchLabelName`).
- Try-exit label names (`TryExitLabelName`).
- Scope indices (`ScopeIndex.Indexed`).

`SimpleFreshEntityProducer<R>` is a specialisation where no seed value is needed.

---

## ScopeIndex

`ScopeIndex` is a sealed class used to track scope nesting during conversion:

- `ScopeIndex.Indexed(n)`: a normal scope at depth `n`; local variables can be declared.
- `ScopeIndex.NoScope`: a scope that prohibits local variable creation, used inside `forall` quantifier bodies.

---

## StdLibConverter

The stdlib converter intercepts calls to Kotlin standard library higher-order functions and translates them without generating Viper method calls. Intercepted functions include:

- `check(condition)` — translated to a Viper `assert`.
- Scope functions: `run`, `let`, `also`, `with`, `apply` — inlined by passing the receiver or result through the lambda body.

These interceptions happen during `StmtConversionVisitor`'s function-call handling, before falling back to the general call embedding path.

---

## Key source files

| File | Role |
|---|---|
| `ProgramConverter.kt` | Top-level context; owns all registries and assembles `Program` |
| `MethodConverter.kt` | Function-level context; resolves parameters and local variables |
| `StmtConverter.kt` | Statement-level context; dispatches to visitor |
| `StmtConversionContext.kt` | Interface for statement-level conversion capabilities |
| `AccessPolicy.kt` | Enum for field access permissions |
| `FreshEntityProducer.kt` | Unique name/index generator |
| `ScopeIndex.kt` | Scope depth tracker |
