# Embeddings

The embeddings layer is the **core abstraction** between Kotlin's FIR (Frontend Intermediate Representation) and the Viper intermediate verification language.

Every Kotlin construct — expressions, statements, functions, properties, types — is first translated into an *embedding*: a typed, structured object that can later be lowered to Viper AST nodes for verification. The embedding layer is intentionally independent of the Viper AST; it carries enough semantic information to allow correct Viper code generation without being tied to Viper's exact node shapes.

Source: `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/`

---

## `ExpEmbedding` — the sealed interface

`ExpEmbedding` is the fundamental type for all expressions. Every node in the embedding tree implements this interface.

### Core fields and methods

| Member | Purpose |
|---|---|
| `type: TypeEmbedding` | The Kotlin type of the expression's result |
| `sourceRole: SourceRole?` | Optional debugging/diagnostic tag (see below) |
| `isValid(purityContext)` | Structural validity hook — called by `checkValidity` on all nodes |

### Lowering methods

| Method | Meaning |
|---|---|
| `toViper(ctx)` | Lower to a Viper `Exp` of type `Ref` (the universal Viper heap type) |
| `toViperBuiltinType(ctx)` | Lower to a Viper `Exp` of a built-in type (Bool or Int) |
| `toViperStoringIn(result, ctx)` | Evaluate and store the result in the given variable |
| `toViperMaybeStoringIn(result?, ctx)` | Store only if `result` is non-null |
| `toViperUnusedResult(ctx)` | Evaluate and discard the result (statement context) |

### Mixin traits

Rather than implementing all lowering methods in every node, most nodes inherit one mixin trait that provides correct default implementations:

| Mixin | Use case |
|---|---|
| `DirectResultExpEmbedding` | Result is available as a pure Viper expression inline |
| `StoredResultExpEmbedding` | Result must be stored in a fresh variable before use |
| `OptionalResultExpEmbedding` | Different semantics depending on whether storage is requested |
| `NoResultExpEmbedding` | Never produces a value (e.g., `return`, `break`) |
| `UnitResultExpEmbedding` | Always produces `Unit` |
| `PassthroughExpEmbedding` | Transparent wrapper that delegates all methods to an inner embedding |

### `SourceRole`

`SourceRole` is a sealed interface tagging each `ExpEmbedding` with its *purpose* in the original Kotlin source. It is used exclusively for debugging and producing precise diagnostic messages — it has no effect on verification.

Key subtypes:

| Tag | Meaning |
|---|---|
| `ReturnsEffect.Wildcard/Null/Bool` | A `returns(...)` contract effect |
| `ConditionalEffect` | A `returns(...) implies ...` effect |
| `Condition.IsNull / IsType` | A null or type predicate in a contract condition |
| `Condition.Conjunction / Disjunction / Negation` | Logical compound condition |
| `ListElementAccessCheck` | Bounds-check assertion on list indexing |

---

## Expression subtypes

### Variables

| Type | Used for |
|---|---|
| `PlaceholderVariableEmbedding` | Function parameters and result variables |
| `AnonymousVariableEmbedding` | Compiler-generated temporaries of type `Ref` |
| `AnonymousBuiltinVariableEmbedding` | Compiler-generated temporaries of type Int or Bool |
| `FirVariableEmbedding` | Local variables originating from Kotlin source |
| `LinearizationVariableEmbedding` | Variables introduced during the linearization pass |

`underlyingVariable` is a helper that unwraps `Cast`/wrapper nodes to reach the actual variable embedding.

### Literals

`LiteralEmbedding` subtypes: `IntLit`, `BooleanLit`, `CharLit`, `StringLit`, `NullLit`, `UnitLit`.

All literals are `DirectResultExpEmbedding`s — they lower directly to the appropriate Viper literal expression.

### Control flow

| Node | Kotlin construct |
|---|---|
| `Block` | `{ ... }` — sequence of expressions; type = type of last element |
| `If` | `if (...) ... else ...` |
| `While` | `while (...) { ... }` with optional loop invariants |
| `Goto` / `LabelExp` | Synthetic labels used for `break`, `continue`, and `return` jumps |
| `GotoChainNode` | Goto with optional result storage |
| `FunctionExp` | A full function body with its associated return label |
| `Return` | Return expression; jumps to the enclosing `FunctionExp`'s return label |
| `Elvis` | `a ?: b` — null-coalescing operator |
| `MethodCall` | A Viper method call (for impure functions) |
| `FunctionCall` | A Viper function call (for `@Pure` functions) |
| `InvokeFunctionObject` | Call to a function-typed value (unknown implementation) |
| `NonDeterministically` | Non-deterministic branch via Viper `if(*)` |

### Operators

Operators are defined using a builder DSL (`OperatorExpEmbeddingBuilder`):

```kotlin
val AddIntInt = buildBinaryOperator("add") {
    signature { withParam { int() }; withParam { int() }; withReturnType { int() } }
    viperOp { l, r -> Add(l, r) }
}
```

Predefined operator families:

| Family | Operators |
|---|---|
| Integer arithmetic | `AddIntInt`, `SubIntInt`, `MulIntInt`, `DivIntInt`, `RemIntInt` |
| Integer comparison | `LeIntInt`, `LtIntInt`, `GeIntInt`, `GtIntInt` |
| Boolean logic | `Not`, `And`, `Or`, `Implies` |
| Character arithmetic | `SubCharChar`, `AddCharInt`, `SubCharInt` |
| Character comparison | `LeCharChar`, `LtCharChar`, `GeCharChar`, `GtCharChar` |
| String | `StringLength`, `StringGet`, `AddStringString`, `AddStringChar` |

`And` / `Or` here are **non-short-circuit** operators for use in Viper specifications. Short-circuit `&&` / `||` from Kotlin source become `SequentialAnd` / `SequentialOr`.

### Sequential logic

`SequentialAnd` and `SequentialOr` model Kotlin's short-circuit `&&` and `||`:

- `SequentialAnd(left, right)`: the right operand is evaluated only when the left is `true`.
- `SequentialOr(left, right)`: the right operand is evaluated only when the left is `false`.

In a **pure expression context** (inside a `@Pure` function or quantifier body), they lower to the non-short-circuit `And`/`Or` Viper expressions. In a **statement context**, they lower to `if`-based control flow to preserve the side-effect semantics.

### Type operations

| Node | Kotlin construct |
|---|---|
| `Is` | `x is T` — runtime type check |
| `Cast` | Unsafe cast; changes the type annotation without changing the value |
| `SafeCast` | `x as? T` — returns a nullable type |
| `InhaleInvariants` | Augment an expression with its type invariants (inhale into current scope) |

### Miscellaneous nodes

| Node | Purpose |
|---|---|
| `Assign` | Variable assignment |
| `Declare` | Variable declaration with optional initializer |
| `FieldAccess` | Read a heap field (requires read permission) |
| `PrimitiveFieldAccess` | Read a field that needs no heap permission |
| `FieldModification` | Write a heap field |
| `FieldAccessPermissions` | Assert field access permission in a specification |
| `PredicateAccessPermissions` | Assert predicate access permission in a specification |
| `Assert` | Viper `assert` — condition must be a pure expression (enforced by `checkValidity`) |
| `InhaleDirect` | Directly inhale an expression into the current state |
| `WithPosition` | Attaches a `KtSourceElement` source position to an embedding |
| `SharingContext` / `Shared` | Memoize a sub-expression to avoid duplicating side effects |
| `ForAllEmbedding` | Viper `forall` quantifier |
| `LambdaExp` | Lambda or anonymous function |
| `EqCmp`, `NeCmp` | Structural equality and inequality |

---

## `TypeEmbedding`

See [Type System](type-system.md) for a full description. In brief:

- `PretypeEmbedding` — the structural type: `Int`, `Bool`, `Char`, `String`, a class, a function type, `Unit`, `Nothing`, `Any`.
- `TypeEmbedding` — a pretype plus `TypeEmbeddingFlags` (nullable, unique).
- Built-in pretypes (Int, Bool, Char) have a **type injection**: a pair of Viper domain functions that inject/project values between Viper's `Ref` universe and the built-in `Int`/`Bool` types.

---

## `FunctionEmbedding` hierarchy

```
CallableEmbedding
└── FunctionEmbedding
    ├── RichCallableEmbedding
    │   ├── NonInlineNamedFunction
    │   └── InlineNamedFunction
    └── SpecialKotlinFunction
        ├── FullySpecialKotlinFunction
        └── PartiallySpecialKotlinFunction
```

### `CallableEmbedding`

The root interface. Every callable must provide:
- `callableType: FunctionTypeEmbedding`
- `insertCall(args, ctx): ExpEmbedding` — generates the embedding for a call site.

### `FunctionEmbedding`

Adds `viperMethod: Method?`, which is `null` for inline or special functions that do not emit a standalone Viper method.

### `FullNamedFunctionSignature`

Extends the signature interface with:
- `getPreconditions()` — list of precondition `ExpEmbedding`s.
- `getPostconditions(returnVariable)` — postconditions with the return value placeholder substituted.
- `toViperMethod()` / `toViperFunction()` — final Viper AST assembly.

### Special stdlib functions

Many Kotlin stdlib operations have no meaningful Viper method representation and must be translated directly to Viper expressions.

**`FullySpecialKotlinFunction`**: functions that are *always* translated to a Viper expression. No Viper method is emitted. Examples:

| Kotlin | Viper embedding |
|---|---|
| `Int.plus(Int)` | `AddIntInt` |
| `Int.compareTo(Int)` | `LtIntInt` / `GeIntInt` / ... |
| `Boolean.not()` | `Not(args[0])` |
| `String.length` | `StringLength` |
| `Char.code` | Identity (built-in type injection) |

**`PartiallySpecialKotlinFunction`**: functions that can sometimes be translated specially, falling back to a normal method call otherwise. Currently only `String.plus(Any)`:
- If the argument is a `String` → `AddStringString`
- If the argument is a `Char` → `AddStringChar`
- Otherwise → fall back to the base `FunctionEmbedding`

---

## Key architectural patterns

### Visitor pattern

All expression traversal uses `ExpVisitor<R>`, an interface with one `visitX` method per `ExpEmbedding` subtype. No `when (embedding is ...)` chains appear in traversal code. The purity checker (`ExprPurityVisitor`), debug printer, and linearizers all implement `ExpVisitor`.

### Mixin traits for lowering semantics

Rather than implementing `toViper` / `toViperStoringIn` / ... in every node, most nodes inherit one mixin trait that provides the correct default implementations based on the node's result semantics. This prevents accidental divergence between the lowering methods.

### Builder DSLs

Types (`TypeBuilder`, `PretypeBuilder`) and operators (`OperatorExpEmbeddingBuilder`) are defined via small internal DSLs. This keeps definitions concise and co-located with their semantic meaning.

### Lazy initialization for classes

`ClassTypeEmbedding` separates creation from initialization. Class embeddings are registered when first referenced and fully initialized (with fields, supertypes, and predicates) in a later pass. This allows mutually-recursive type hierarchies to be processed without requiring a strict ordering.
