# Embeddings

This package is the **core abstraction layer** between Kotlin's FIR (Frontend Intermediate
Representation) and the Viper intermediate verification language.

Every Kotlin construct — expressions, statements, functions, properties, types — is first
translated into an *embedding*: a typed, structured object that can later be lowered to Viper
AST nodes for verification. The embedding layer is intentionally independent of the Viper AST;
it carries enough semantic information to allow correct Viper code generation without being
tied to Viper's exact node shapes.

---

## Directory layout

```
embeddings/
├── EMBEDDINGS.md                       ← this file
├── ExpVisitor.kt                       ← visitor interface for expression trees
├── FunctionBodyEmbedding.kt            ← function body wrapper + Viper method builder
├── LabelEmbedding.kt                   ← control-flow labels and goto links
├── SourceRole.kt                       ← source-level role tags for debugging/errors
├── callables/                          ← callable entities (functions, specials, lambdas)
├── expression/                         ← expression embeddings and operators
├── properties/                         ← property/field embeddings
└── types/                              ← type system embeddings and invariants
```

---

## Root-level files

### `ExpVisitor.kt`

A visitor interface (`ExpVisitor<R>`) with one `visitX` method per `ExpEmbedding` subtype.
All expression traversal in the compiler plugin goes through this interface, enabling
double-dispatch without `when`/`is` chains in calling code.

The purity checker (`ExprPurityVisitor`), debug printer, and various linearizers all implement
`ExpVisitor`.

### `FunctionBodyEmbedding.kt`

A data class wrapping the results of converting a function body:

| Field | Purpose |
|---|---|
| `body: Seqn` | The Viper statement sequence for the body |
| `returnTarget: LabelEmbedding` | The label `return` expressions jump to |
| `debugInfo` | Optional debug annotation |

`toViperMethod(signature)` assembles a complete `viper.silver.ast.Method` from a
`FullNamedFunctionSignature` and this body.

### `LabelEmbedding.kt`

Labeled positions in control flow:

- `LabelEmbedding` — a named label that may carry loop invariants. Used as targets for
  `Goto`, `break`, and `continue`.
- `LabelLink` — a lightweight reference to a `LabelEmbedding` used inside `Goto` expressions.

### `SourceRole.kt`

A sealed interface tagging each `ExpEmbedding` with its *purpose* in the original Kotlin source.
Used exclusively for debugging and diagnostic messages — it has no effect on verification.

Key subtypes:

| Tag | Meaning |
|---|---|
| `ReturnsEffect.Wildcard/Null/Bool` | A `returns(...)` contract effect |
| `ConditionalEffect` | A `returns(...) implies ...` effect |
| `Condition.IsNull / IsType` | A null or type predicate in a contract condition |
| `Condition.Conjunction / Disjunction / Negation` | Logical compound conditions |
| `ListElementAccessCheck` | Bounds-check assertion on list indexing |

The extension property `asInfo` converts a `SourceRole` to a Viper `Info` node attached to
the generated AST node.

---

## `callables/` — Callable embeddings

This subdirectory models everything that can be *called*: named functions, inline functions,
lambdas, and the special-cased stdlib functions that map directly to Viper operators.

### Interface hierarchy

```
CallableEmbedding
└── FunctionEmbedding
    ├── RichCallableEmbedding (+ NamedFunctionSignature + FullNamedFunctionSignature)
    │   ├── NonInlineNamedFunction
    │   └── InlineNamedFunction
    └── SpecialKotlinFunction
        ├── FullySpecialKotlinFunction
        └── PartiallySpecialKotlinFunction
```

#### `CallableEmbedding`
The root interface. Every callable must supply:
- `callableType: FunctionTypeEmbedding`
- `insertCall(args, ctx): ExpEmbedding` — generates the expression for a call site.

#### `FunctionEmbedding`
Adds `viperMethod: Method?` (null for inline/special functions).

#### `FunctionSignature`
Provides parameter variables (`dispatchReceiver`, `extensionReceiver`, `params`) and the
`formalArgs` flat list. The `GenericFunctionSignatureMixin` default implementation extracts
these from the `callableType`.

#### `FullNamedFunctionSignature`
Extends `FunctionSignature` with:
- `getPreconditions()` — list of precondition `ExpEmbedding`s
- `getPostconditions(returnVariable)` — postconditions with the return value substituted
- `toViperMethod()` and `toViperFunction()` — final Viper AST assembly

Also defines `PropertyAccessorFunctionSignature` for getters/setters, which have no
user-written contracts.

#### `NamedFunctionSignature`
Adds `name` and the FIR symbol, and provides `toMethodCall()` / `toFuncApp()` for
generating Viper call expressions.

---

### Named function implementations

#### `NonInlineNamedFunction.kt`
A regular (non-inline) user function. `insertCall` emits either a `MethodCall` (impure) or a
`FunctionCall` (pure, if annotated `@Pure`).

#### `InlineNamedFunction.kt`
A function marked `inline`. `insertCall` delegates to
`StmtConversionContext.insertInlineFunctionCall`, which substitutes the body at the call site
rather than emitting a method call.

---

### Special stdlib functions

Many Kotlin stdlib operations have no meaningful Viper method representation and must be
translated directly to Viper expressions. Two tiers exist:

#### `FullySpecialKotlinFunction.kt`
Functions that are **always** translated to a Viper expression — no Viper method is ever
emitted. Each entry is defined via a builder DSL:

```kotlin
addFunction(intToIntType, SpecialPackages.kotlin, className = "Int", name = "plus") { args, _ ->
    AddIntInt(args[0], args[1])
}
```

Examples (all pure in the Viper sense):

| Kotlin | Viper embedding |
|---|---|
| `Int.plus(Int)` | `AddIntInt` |
| `Int.compareTo(Int)` | `LtIntInt` / `GeIntInt` / … |
| `Boolean.not()` | `Not(args[0])` |
| `String.length` | `StringLength` |
| `Char.code` | identity (built-in type injection) |

`FullySpecialKotlinFunctionBuilder` and `SpecialFunctions.kt` hold the complete registry.

#### `PartiallySpecialKotlinFunction.kt`
Functions that can be translated specially *only in some cases*, falling back to a regular
method call otherwise. Currently only `String.plus(Any)`:
- If the argument is a `String` → `AddStringString`
- If the argument is a `Char` → `AddStringChar`
- Otherwise → fall back to the base (non-special) `FunctionEmbedding`

---

## `expression/` — Expression embeddings

This is the largest subdirectory. It defines every `ExpEmbedding` node type.

### `ExpEmbedding.kt` — the sealed interface

`ExpEmbedding` is the fundamental type for all expressions. It carries:
- `type: TypeEmbedding` — the Kotlin type of the result
- `sourceRole: SourceRole?` — optional debugging tag
- `isValid(purityContext)` — structural validity hook (see `purity/PURITY.md`)

Core lowering methods:

| Method | Meaning |
|---|---|
| `toViper(ctx)` | Lower to a Viper `Exp` of type `Ref` |
| `toViperBuiltinType(ctx)` | Lower to a Viper `Exp` of a built-in type (Bool, Int) |
| `toViperStoringIn(result, ctx)` | Evaluate and store result in `result` variable |
| `toViperMaybeStoringIn(result?, ctx)` | Store only if `result` is non-null |
| `toViperUnusedResult(ctx)` | Evaluate and discard result (statement context) |

Most expression types inherit one of several **mixin traits** that provide default
implementations of the lowering methods:

| Mixin | Use case |
|---|---|
| `DirectResultExpEmbedding` | Result available as a pure Viper `Exp` |
| `StoredResultExpEmbedding` | Result must be stored in a fresh variable |
| `OptionalResultExpEmbedding` | Different semantics with/without storage |
| `NoResultExpEmbedding` | Never produces a value (e.g., `return`, `break`) |
| `UnitResultExpEmbedding` | Always produces `Unit` |
| `PassthroughExpEmbedding` | Transparent wrapper delegating to inner embedding |

Helper nodes:

- `ExpWrapper` — wraps a raw Viper `Exp` with a `TypeEmbedding`; used when Viper already
  has the right expression shape.
- `ErrorExp` — sentinel for unreachable code; emits `inhale false`.

---

### Control flow (`ControlFlow.kt`)

| Node | Kotlin construct |
|---|---|
| `Block` | `{ ... }` — sequence; type = last element |
| `If` | `if (...) ... else ...` |
| `While` | `while (...) { ... }` with invariants |
| `Goto` / `LabelExp` | Synthetic labels for `break`/`continue`/`return` |
| `GotoChainNode` | Goto with optional result storage |
| `FunctionExp` | Full function body with return label |
| `Return` | Return expression; jumps to enclosing `FunctionExp`'s label |
| `Elvis` | `a ?: b` — null-coalescing |
| `MethodCall` | Viper method call (impure) |
| `FunctionCall` | Viper function call (pure) |
| `InvokeFunctionObject` | Call to a function-typed value (unknown impl) |
| `NonDeterministically` | Nondeterministic branch via Viper `if(*)` |

---

### Variables (`VariableEmbedding.kt`)

All variable-like entities implement `VariableEmbedding`:

| Type | Used for |
|---|---|
| `PlaceholderVariableEmbedding` | Parameters, result variables |
| `AnonymousVariableEmbedding` | Compiler-generated temporaries (Ref type) |
| `AnonymousBuiltinVariableEmbedding` | Compiler-generated temporaries (Int/Bool) |
| `FirVariableEmbedding` | Local variables from Kotlin source |
| `LinearizationVariableEmbedding` | Variables introduced during linearization |

The `underlyingVariable` helper unwraps cast/wrapper nodes to reach the variable directly.

---

### Literals (`Literal.kt`)

`LiteralEmbedding` subtypes: `IntLit`, `BooleanLit`, `CharLit`, `StringLit`, `NullLit`, `UnitLit`.
All are `DirectResultExpEmbedding`s — they lower directly to the appropriate Viper expression.

---

### Operators (`OperatorExpEmbeddings.kt` and friends)

Operators are defined using a builder DSL (`OperatorExpEmbeddingBuilder`):

```kotlin
val AddIntInt = buildBinaryOperator("add") {
    signature { withParam { int() }; withParam { int() }; withReturnType { int() } }
    viperOp { l, r -> Add(l, r) }
}
```

`InjectionBasedExpEmbedding` handles the type-injection plumbing that converts Kotlin's
`Ref`-based representation to Viper's built-in `Int`/`Bool` types and back.

Predefined operator families in `OperatorExpEmbeddings`:

| Family | Operators |
|---|---|
| Integer arithmetic | `AddIntInt`, `SubIntInt`, `MulIntInt`, `DivIntInt`, `RemIntInt` |
| Integer comparison | `LeIntInt`, `LtIntInt`, `GeIntInt`, `GtIntInt` |
| Boolean logic | `Not`, `And`, `Or`, `Implies` |
| Character arithmetic | `SubCharChar`, `AddCharInt`, `SubCharInt` |
| Character comparison | `LeCharChar`, `LtCharChar`, `GeCharChar`, `GtCharChar` |
| String | `StringLength`, `StringGet`, `AddStringString`, `AddStringChar` |

Note: `And` / `Or` here are **non-short-circuit** operators used in Viper specifications
(contracts, quantifiers). The short-circuit `&&` / `||` from Kotlin source becomes
`SequentialAnd` / `SequentialOr` (see below).

---

### Sequential logic (`SequentialLogicOperatorEmbedding.kt`)

`SequentialAnd` and `SequentialOr` model Kotlin's short-circuit `&&` and `||`:
- `SequentialAnd(left, right)`: right operand evaluated only when left is `true`.
- `SequentialOr(left, right)`: right operand evaluated only when left is `false`.

Both are **structurally delegating** in the purity visitor — pure iff both operands are pure.

In an expression context (e.g., inside a pure function or quantifier), they can be lowered to
the non-short-circuit `And`/`Or` Viper expressions. In a statement context they lower to
`if`-based control flow.

---

### Type operations (`TypeOp.kt`)

| Node | Kotlin construct |
|---|---|
| `Is` | `x is T` — type check |
| `Cast` | Unsafe cast; changes the type annotation without changing the value |
| `SafeCast` | `x as? T` — returns nullable type |
| `InhaleInvariants` | Augment expression with type invariants (inhale into scope) |

---

### Miscellaneous expression nodes

| Node | Purpose |
|---|---|
| `Assign` | Variable assignment |
| `Declare` | Variable declaration with optional initializer |
| `FieldAccess` | Read a field (with permission) |
| `PrimitiveFieldAccess` | Read a field (no permission needed) |
| `FieldModification` | Write a field |
| `FieldAccessPermissions` | Assert field access permission |
| `PredicateAccessPermissions` | Assert predicate access permission |
| `Assert` | Viper `assert` — condition must be pure (enforced by `checkValidity`) |
| `InhaleDirect` | Directly inhale an expression |
| `WithPosition` | Attaches a `KtSourceElement` source position |
| `SharingContext` / `Shared` | Memoize a sub-expression to avoid duplication |
| `ForAllEmbedding` | Viper `forall` quantifier |
| `LambdaExp` | Lambda / anonymous function |
| `EqCmp`, `NeCmp` | Equality / inequality comparisons |

---

## `properties/` — Property and field embeddings

Kotlin properties are modelled separately from their backing storage.

### Key interfaces

| Interface | Role |
|---|---|
| `PropertyAccessEmbedding` | Abstract getter + setter for a property |
| `GetterEmbedding` | `getValue(receiver, ctx): ExpEmbedding` |
| `SetterEmbedding` | `setValue(receiver, value, ctx): ExpEmbedding` |
| `FieldEmbedding` | The Viper field backing a property |

### Field access policy

`FieldEmbedding` carries an `accessPolicy` controlling permission requirements and whether
invariants need to be unfolded for access. The invariant queries
`accessInvariantsForParameter()` and `accessInvariantForAccess()` feed into preconditions
and pre-access checks respectively.

### Implementations

| Class | Purpose |
|---|---|
| `BackingFieldGetter` / `BackingFieldSetter` | Direct field read/write |
| `CustomGetter` / `CustomSetter` | Calls a user-defined getter/setter method |
| `ClassPropertyAccess` | Property access on a class instance, including type invariants |
| `LengthFieldGetter` | Special getter for `String.length` |

---

## `types/` — Type embeddings

The type system in SnaKt is split across two levels:

### `PretypeEmbedding` — structural type

A `PretypeEmbedding` describes the core structure of a type, independent of nullability:

| Pretype | Kotlin type |
|---|---|
| `UnitTypeEmbedding` | `Unit` |
| `NothingTypeEmbedding` | `Nothing` |
| `AnyTypeEmbedding` | `Any` |
| `IntTypeEmbedding` | `Int` |
| `BooleanTypeEmbedding` | `Boolean` |
| `CharTypeEmbedding` | `Char` |
| `StringTypeEmbedding` | `String` |
| `ClassTypeEmbedding` | user-defined class or interface |
| `FunctionTypeEmbedding` | function type `(A, B) -> C` |

Built-in pretypes (Int, Bool, Char) have an associated *type injection* — a pair of functions
that inject/project the value between Viper's `Ref` universe and the built-in `Int`/`Bool`
types.

### `TypeEmbedding` — full type with flags

`TypeEmbedding` wraps a `PretypeEmbedding` with `TypeEmbeddingFlags` (nullability, uniqueness,
etc.). Methods:
- `getNullable()` / `getNonNullable()` — produce the nullable/non-nullable variant.
- `injection` / `injectionOrNull` — type injection for built-ins.
- Invariant queries delegate to the pretype with flag adjustments.

Builders:
- `buildType { ... }` / `TypeBuilder` DSL
- `buildFunctionPretype { ... }` for function types
- `buildClassPretype { ... }` for class types

### `ClassTypeEmbedding` and `ClassEmbeddingDetails`

Class types use lazy initialisation (via `initDetails()`) to handle forward references
during class embedding:

- `ClassEmbeddingDetails` stores `superTypes`, `fields`, and the Viper predicates
  (`sharedPredicate`, `uniquePredicate`) encoding class invariants.
- `hierarchyUnfoldPath()` — computes the predicate unfolding path through the inheritance
  hierarchy, needed before accessing a field behind a predicate.
- `accessInvariants()` — assembles the full set of field access requirements for verification.

### Type invariants

`TypeInvariantEmbedding` represents a single constraint that must hold for a value of some type:

| Invariant type | Meaning |
|---|---|
| `SubTypeInvariantEmbedding` | Value must pass an `is T` check |
| `FieldAccessTypeInvariantEmbedding` | Field permission must be held |
| `PredicateAccessTypeInvariantEmbedding` | Predicate permission must be held |
| `FieldEqualsInvariant` | A field must equal a specific value |
| `IfNonNullInvariant` | Condition applies only when the value is non-null |
| `FalseTypeInvariant` | Unsatisfiable — used for `Nothing` |

`TypeInvariantHolder` is the interface for anything (type, field, class) that can supply
`accessInvariants()`, `pureInvariants()`, and predicate invariants.

---

## Key architectural patterns

### 1. Visitor pattern
All expression traversal uses `ExpVisitor<R>`. No `when (embedding is ...)` chains appear in
traversal code; instead, each node type implements `accept(v)` and calls the appropriate
`visitX` method.

### 2. Mixin traits for lowering semantics
Rather than implementing `toViper` / `toViperStoringIn` / … in every node, most nodes inherit
one mixin trait (`DirectResultExpEmbedding`, `StoredResultExpEmbedding`, etc.) that provides
the correct default implementations given the node's result semantics.

### 3. Builder DSLs for types and operators
Types (`TypeBuilder`, `PretypeBuilder`) and operators (`OperatorExpEmbeddingBuilder`) are
defined via small internal DSLs. This keeps the definitions concise and co-located with their
semantic meaning rather than scattered across factory methods.

### 4. Lazy initialisation for classes
`ClassTypeEmbedding` and `ClassEmbeddingDetails` separate *creation* from *initialisation*.
Class embeddings are created when first referenced and fully initialised (fields, supertypes,
predicates) in a later pass. This allows handling mutually-recursive type hierarchies.

### 5. Special-function registry
Kotlin stdlib functions that correspond to primitive Viper operations are intercepted before
call-site embedding is generated. The two-tier system (`FullySpecialKotlinFunction` /
`PartiallySpecialKotlinFunction`) handles the common case (always special) separately from
the rare case (sometimes special, sometimes a real call).
