# Viper AST

The Viper AST module lives in `formver.compiler-plugin/viper/src/…/viper/` and provides:

1. A complete Kotlin-side representation of the Viper AST (mirroring `viper.silver.ast`).
2. The `IntoSilver` interface and conversions to the Silver representation consumed by Silicon.
3. The `Verifier` class that calls Silicon and returns errors.

---

## Overview of the Viper language

Viper is a verification intermediate language that supports:

- **Methods**: impure procedures with pre/postconditions and statement bodies.
- **Functions**: pure, side-effect-free computations usable in specifications.
- **Predicates**: recursive permission abstractions that hide heap structure.
- **Domains**: algebraic structures with domain functions and axioms.
- **Fields**: heap fields shared by all reference-typed objects.

Viper uses *separation logic* for heap reasoning: to read or write a field you must hold a fractional or full permission to it. Permissions are transferred between caller and callee via `inhale`/`exhale` in preconditions and postconditions.

---

## Program

`Program` is the root of the Viper AST — a complete, self-contained Viper program ready for verification.

```kotlin
data class Program(
    val domains: List<Domain>,
    val fields: List<Field>,
    val functions: List<Function>,
    val predicates: List<Predicate>,
    val methods: List<Method>,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
)
```

- `domains`: algebraic data-type definitions used for runtime type encoding.
- `fields`: heap fields shared across all objects (one declaration per field name).
- `functions`: pure Viper functions usable in specifications.
- `predicates`: recursive permission abstractions (shared/unique access predicates per class).
- `methods`: impure Viper methods with pre/postconditions and statement bodies.

`toSilver()` converts the program to `viper.silver.ast.Program` by sorting each list by mangled name (for deterministic output) and recursively converting every node. A `NameResolver` context is required to map symbolic names to strings.

`toShort()` and `withoutPredicates()` produce filtered copies for diagnostic logging, omitting internal boilerplate that is not relevant to the user.

---

## Method

A `Method` represents a Viper method (an impure callable):

```
method methodName(args) returns (rets)
  requires precondition
  ensures  postcondition
{
  body
}
```

Key fields:

| Field | Type | Meaning |
|---|---|---|
| `name` | `SymbolicName` | Unique method identifier (mangled Kotlin name) |
| `formalArgs` | `List<Declaration.LocalVarDecl>` | Formal input parameters |
| `formalReturns` | `List<Declaration.LocalVarDecl>` | Return variables (Viper methods return via out-parameters) |
| `pres` | `List<Exp>` | Preconditions (`requires`) |
| `posts` | `List<Exp>` | Postconditions (`ensures`) |
| `body` | `Stmt.Seqn?` | The method body; null for abstract/extern methods |

---

## Function

A `Function` represents a Viper pure function:

```
function functionName(args): ReturnType
  requires precondition
  ensures  postcondition
{ body }
```

Unlike methods, Viper functions:
- Are deterministic and side-effect-free.
- Can be called inside specifications (preconditions, postconditions, invariants).
- Have an `Exp` body (not a `Seqn`).
- May use `unfolding P(x) in e` to peek inside predicates.

Key fields: `name`, `formalArgs`, `type` (return type), `pres`, `posts`, `body: Exp?`.

---

## Domain

A `Domain` is an abstract algebraic structure used for runtime type encoding:

```
domain DomainName {
  function domainFun(args): ReturnType
  axiom { forall x: T :: ... }
}
```

Domains provide:
- Uninterpreted functions (`DomainFunc`) that the SMT solver treats as abstract.
- Axioms (`DomainAxiom`) that constrain the functions.

SnaKt uses domains to encode the Kotlin type hierarchy (`RuntimeTypeDomain`): each class gets a domain function representing its runtime type, and axioms encode subtype relationships.

Key fields: `name: SymbolicName`, `functions: List<DomainFunc>`, `axioms: List<DomainAxiom>`, `typVars: List<Type.TypeVar>`.

---

## Field

A `Field` is a heap location:

```
field fieldName: FieldType
```

All objects in Viper share the same set of fields. Permission to a field is required before reading or writing it.

Key fields: `name: SymbolicName`, `type: Type`.

---

## Predicate

A `Predicate` is a recursive permission abstraction:

```
predicate predicateName(args) {
  body
}
```

Predicates in SnaKt are used to bundle the access permissions for all fields of a class into a single named predicate. Two predicates are generated per class:

- `sharedPredicate`: fractional (read) access to all fields.
- `uniquePredicate`: full (write) access to all fields.

Key fields: `name: SymbolicName`, `formalArgs: List<Declaration.LocalVarDecl>`, `body: Exp`.

---

## Stmt (statement hierarchy)

`Stmt` is a sealed interface. All statement types implement `IntoSilver<viper.silver.ast.Stmt>`.

| Subtype | Description |
|---|---|
| `Stmt.Seqn` | Statement sequence; holds a list of statements and scoped declarations |
| `Stmt.If` | Conditional branch: `if (cond) { then } else { els }` |
| `Stmt.While` | Loop with loop invariants |
| `Stmt.Label` | A named label (target for `goto`) |
| `Stmt.Goto` | Jump to a label |
| `Stmt.LocalVarAssign` | Assignment to a local variable |
| `Stmt.FieldAssign` | Assignment to a heap field |
| `Stmt.MethodCall` | Call to an impure method; targets are out-parameter variables |
| `Stmt.Exhale` | Relinquish a permission or assertion |
| `Stmt.Inhale` | Acquire a permission or assertion |
| `Stmt.Assert` | Assert that an expression holds |
| `Stmt.Fold` | Fold a predicate (package up field permissions into the predicate) |
| `Stmt.Unfold` | Unfold a predicate (unpackage field permissions from the predicate) |

`Stmt.assign(lhs, rhs)` is a factory that dispatches to `LocalVarAssign` or `FieldAssign` based on `lhs`.

`Stmt.Seqn` holds both `stmts: List<Stmt>` (ordered statements) and `scopedSeqnDeclarations: List<Declaration>` (local variable and label declarations visible within this block). This mirrors the Silver AST structure exactly.

---

## Exp (expression hierarchy)

`Exp` is a sealed interface. All expression types carry a `type: Type` and implement `IntoSilver<viper.silver.ast.Exp>`.

### Literals

| Type | Viper type |
|---|---|
| `Exp.IntLit(value)` | `Int` |
| `Exp.BoolLit(value)` | `Bool` |
| `Exp.NullLit` | `Ref` |

### Variables and field access

- `Exp.LocalVar(name, type)`: a local variable reference.
- `Exp.FieldAccess(rcv, field)`: heap field read.
- `Exp.Result(type)`: the implicit `result` variable in postconditions.

### Arithmetic and comparison

`Exp.Add`, `Exp.Sub`, `Exp.Mul`, `Exp.Div`, `Exp.Mod`, `Exp.Minus` (unary negation).

`Exp.LtCmp`, `Exp.LeCmp`, `Exp.GtCmp`, `Exp.GeCmp`, `Exp.EqCmp`, `Exp.NeCmp`.

### Boolean operators

`Exp.And`, `Exp.Or`, `Exp.Implies`, `Exp.Not`.

### Quantifiers

- `Exp.Forall(variables, triggers, exp)`: universal quantification.
- `Exp.Exists(variables, triggers, exp)`: existential quantification.
- `Exp.ForallBuilder`: DSL for building `forall x :: { trigger } assumptions ==> conclusion`.

### Sequence operations (used for strings and lists)

`Exp.ExplicitSeq`, `Exp.EmptySeq`, `Exp.SeqLength`, `Exp.SeqIndex`, `Exp.SeqTake`, `Exp.SeqAppend`.

### Specification expressions

- `Exp.PredicateAccess(predicateName, formalArgs, perm)`: predicate access expression (used in `fold`/`unfold` and as assertions).
- `Exp.Acc(field, perm)`: field access predicate (permission assertion).
- `Exp.Unfolding(predicateAccess, body)`: `unfolding P(x) in e` — peek inside a predicate without changing permissions.
- `Exp.Old(exp)`: `old(e)` — value of `e` in the pre-state.

### Control and binding

- `Exp.TernaryExp(cond, then, else)`: conditional expression `cond ? then : else`.
- `Exp.LetBinding(variable, varExp, body)`: `let x == e in body` — SSA let-binding.

### Domain and function calls

- `Exp.FuncApp(functionName, args, type)`: call to a Viper pure function.
- `Exp.DomainFuncApp(function, args, typeVarMap)`: call to a domain function with type variable instantiation.

### Operator extensions

The file also defines Kotlin infix/operator extensions (`Exp.and`, `Exp.or`, `Exp.eq`, `+`, `-`, etc.) so that AST construction code reads naturally.

---

## Type

`Type` is a sealed class representing Viper types:

| Type | Viper | Used for |
|---|---|---|
| `Type.Int` | `Int` | Kotlin `Int` and `Char` (chars as code points) |
| `Type.Bool` | `Bool` | Kotlin `Boolean` |
| `Type.Ref` | `Ref` | All reference types (Kotlin classes, `String`) |
| `Type.Perm` | `Perm` | Permission amounts |
| `Type.TypeVar(name)` | Type variable | Polymorphic domains |
| `Type.Seq(elementType)` | `Seq[T]` | Kotlin `String` and `List` |

---

## IntoSilver

`IntoSilver<T>` is an interface implemented by every Viper AST node:

```kotlin
interface IntoSilver<out T> {
    context(nameResolver: NameResolver)
    fun toSilver(): T
}
```

The `context(nameResolver: NameResolver)` receiver provides the `NameResolver` needed to convert `SymbolicName` values to their mangled string representations. The name resolver is registered once (via `registerAllNames`) before conversion begins.

Helper extensions `List<T>.toSilver()` and `Option<T>.toSilver()` simplify bulk conversion.

---

## Verifier

`Verifier` is the bridge to Silicon (the Viper verifier):

```kotlin
class Verifier {
    fun checkConsistency(viperProgram, onFailure): Boolean
    fun verify(viperProgram, onFailure)
}
```

Initialization creates a `DefaultMainVerifier` (Silicon's main entry point) with a config that ignores the dummy file requirement.

### checkConsistency

Calls `viperProgram.checkTransitively()` (Silver's built-in consistency check) and calls `onFailure` for each `ConsistencyError`. Returns `true` if no errors were found. Consistency errors indicate plugin bugs rather than user code issues.

### verify

Calls `verifier.verify(viperProgram, …)` and calls `onFailure` for each fatal `VerificationError`. A `VerificationError` wraps a `viper.silicon.interfaces.Failure` and provides:

- `id`: the Silicon error class identifier (e.g. `"postcondition.violated:assertion.false"`).
- `msg`: a human-readable message from Silicon.
- `position`: the Viper source position where the error occurred.
- `locationNode`: the Viper AST node where the error occurred.
- `unverifiableProposition`: the failing assertion expression.

### VerifierError

`VerifierError` is a sealed interface with two implementations:

- `VerificationError`: a violation reported by Silicon (a postcondition was not proved, an assertion failed, etc.).
- `ConsistencyError`: a structural inconsistency in the Viper program (type errors, undeclared identifiers, etc.).

---

## Key source files

| File | Role |
|---|---|
| `ast/Program.kt` | Top-level `Program` data class and name registration |
| `ast/Method.kt` | Viper method with pre/postconditions |
| `ast/Function.kt` | Pure Viper function |
| `ast/Domain.kt` | Algebraic domain with functions and axioms |
| `ast/Stmt.kt` | Sealed statement hierarchy |
| `ast/Exp.kt` | Sealed expression hierarchy |
| `ast/Type.kt` | Viper types |
| `ast/Field.kt` | Heap field declaration |
| `ast/Predicate.kt` | Predicate declaration |
| `IntoSilver.kt` | Conversion interface to Silver AST |
| `Verifier.kt` | Silicon interface: consistency check + verification |
| `errors/VerifierError.kt` | Sealed error hierarchy |
| `errors/VerificationError.kt` | Wraps Silicon failure results |
| `errors/ConsistencyError.kt` | Wraps Silver consistency errors |
