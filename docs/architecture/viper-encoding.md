# Common Elements in a Viper Encoding

## Purpose

This document explains the common structural elements that appear in generated Viper encodings, especially encodings that model a higher-level language with:

- runtime types
- boxed primitive values
- subtype and nullability reasoning
- specification functions for primitive operators
- lowered functions and methods with generated control flow

It is written as a practical reference for reading and debugging generated Viper programs.

---

## 1. Big Picture

A generated Viper encoding often has three layers:

1. **Runtime model layer**
   - defines an abstract model of types and values
   - introduces subtype and nullability relations
   - defines boxing and unboxing functions

2. **Primitive semantics layer**
   - gives logical meaning to built-in operations such as integer addition, comparisons, boolean connectives, and string operations

3. **Program layer**
   - contains the translated functions and methods from the source program
   - lowers source-level expressions into Viper expressions or statements
   - may introduce generated temporaries, labels, and jumps

A good way to read a Viper encoding is to identify these three layers first.

---

## 2. Runtime Type Domain

A common pattern is a domain such as:

```viper
domain d$rt {
  ...
}
```

This domain is not an executable data structure. It is a **logical model** used by the verifier.

Typical responsibilities of the runtime domain:

- represent source-language types
- define subtype relations
- represent nullability
- describe the runtime type of boxed values
- connect primitive logical values to boxed `Ref` values

### Best practice when reading

Treat the runtime domain as the specification of the source language's type system inside Viper.

---

## 3. Type Constructors

Generated encodings often contain declarations such as:

```viper
unique function df$rt$intType(): d$rt
unique function df$rt$boolType(): d$rt
unique function df$rt$stringType(): d$rt
```

These are abstract values of type `d$rt` that stand for source-language types.

Common examples:

- `intType()`
- `boolType()`
- `charType()`
- `unitType()`
- `nothingType()`
- `anyType()`
- `functionType()`
- `stringType()`

### Why `unique` matters

`unique` tells Viper that these constructors produce pairwise distinct values. This prevents different source-language types from collapsing into one abstract runtime type.

---

## 4. Distinguished Runtime Values

Many encodings introduce special boxed values:

```viper
function df$rt$nullValue(): Ref
function df$rt$unitValue(): Ref
```

Typical meanings:

- `nullValue()` models the null reference of the source language
- `unitValue()` models the unique unit value

These values are usually constrained further by axioms.

---

## 5. Runtime Type Operations

Common declarations include:

```viper
function df$rt$isSubtype(t1: d$rt, t2: d$rt): Bool
function df$rt$typeOf(r: Ref): d$rt
function df$rt$nullable(t: d$rt): d$rt
```

### `isSubtype`

Represents the source-language subtype relation.

### `typeOf`

Maps a boxed runtime value to its runtime type.

### `nullable`

Builds the nullable variant of a type.

Examples:

- `nullable(Int)` corresponds to a type like `Int?`
- `nullable(String)` corresponds to a type like `String?`

---

## 6. Boxing and Unboxing

A very common encoding technique is to represent all source-language values uniformly as `Ref`, including primitive values.

Typical declarations:

```viper
function df$rt$intToRef(v: Int): Ref
function df$rt$intFromRef(r: Ref): Int
function df$rt$boolToRef(v: Bool): Ref
function df$rt$boolFromRef(r: Ref): Bool
```

This introduces a logical boxing and unboxing layer.

### Why this exists

Many source languages have primitive values and reference values, but the generated Viper encoding may choose a single carrier type, usually `Ref`, for simplicity and uniformity.

### Common invariants

Generated axioms usually establish:

- boxing produces a value of the expected runtime type
- unboxing after boxing returns the original primitive value
- boxing after unboxing returns the original reference for values of the appropriate runtime type

---

## 7. Axioms

The runtime domain is usually accompanied by many axioms. These are central to the encoding.

### 7.1 Subtyping axioms

Typical examples:

- reflexivity
- transitivity
- antisymmetry

These make subtyping behave like a partial order.

### 7.2 Nullability axioms

Typical properties:

- `nullable(nullable(t)) == nullable(t)`
- `t <: nullable(t)`
- if `t1 <: t2`, then `nullable(t1) <: nullable(t2)`

These axioms model nullable types as a lifted version of ordinary types.

### 7.3 Top and bottom types

Common patterns:

- `nothingType()` is a subtype of every type
- every ordinary type is a subtype of `anyType()`
- every type is a subtype of `nullable(anyType())`

This often reflects a source-language hierarchy with:

- `Nothing` as bottom
- `Any` as top among non-nullable types
- `Any?` as top among nullable types

### 7.4 Special values

Typical axioms describe:

- the type of `nullValue()`
- the fact that `nullValue()` is not of type `Any`
- the type of `unitValue()`
- uniqueness of `unitValue()`
- emptiness of `Nothing`

These axioms are what make null and unit behave like source-language values.

### 7.5 Smart-cast style axioms

Some encodings include axioms that help the verifier reason about nullable values.

Typical idea:

- if a value has type `T?`, then either it is null or it has type `T`
- if a type is known to be non-null and also below `T?`, then it is below `T`

These axioms support source-language features such as null checks and smart casts.

### Best practice when reading axioms

Look for the small number of axioms that carry the real semantic weight:

- subtype laws
- nullability laws
- type of null
- type of unit
- boxing and unboxing laws

Those are usually enough to understand the whole runtime model.

---

## 8. Triggers in Quantified Axioms

Generated axioms often contain trigger annotations such as:

```viper
{ df$rt$isSubtype(t1, t2), df$rt$isSubtype(t2, t3) }
```

These are used by the SMT solver to decide when a quantifier should be instantiated.

### Why they matter

Triggers are not the semantic content of the encoding. They are proof-engineering support.

### Best practice when documenting

Explain triggers briefly, but do not let them dominate the explanation unless you are debugging solver behavior.

---

## 9. Specification Functions for Primitive Operations

Generated encodings frequently introduce abstract functions for primitive operations, for example:

```viper
function sp$plusInts(arg1: Ref, arg2: Ref): Ref
  ensures df$rt$isSubtype(df$rt$typeOf(result), df$rt$intType())
  ensures df$rt$intFromRef(result) ==
    df$rt$intFromRef(arg1) + df$rt$intFromRef(arg2)
```

These functions usually do not have bodies. Their behavior is given entirely by postconditions.

Common groups:

- integer arithmetic
- boolean connectives
- comparisons
- character arithmetic
- string concatenation, indexing, and length

### Why this style is useful

It keeps the encoding abstract and avoids re-encoding the full implementation of primitive operations.

### How to read them

Read each primitive function as a specification-level model of a built-in operator.

---

## 10. Translated Functions

A generated source-level function may look like this:

```viper
function f$...(...): Ref
  ensures ...
{
  ...
}
```

Typical characteristics:

- the function is pure
- arguments and result are often boxed as `Ref`
- the body may use `let` expressions
- the body may call specification functions such as `sp$negInt`
- the postcondition usually states the runtime type of the result

### Generated temporaries

Names such as `anon$1$0` are compiler-generated temporaries.

They usually serve one of these purposes:

- preserve evaluation order
- simplify complex expressions
- produce solver-friendly intermediate terms

### Best practice when reading

Ignore the temporary names at first and reconstruct the original source expression.

---

## 11. Translated Methods

A generated source-level method may look like this:

```viper
method f$...(p$a: Ref) returns (ret$0: Ref)
  ensures ...
{
  ...
}
```

Typical characteristics:

- methods are statement-based, not expression-based
- return values are explicit output variables
- the body may contain assignments, assertions, assumptions, labels, and gotos
- method calls in Viper are written as assignments

Example syntax:

```viper
x := someMethod(y)
```

for a single return value.

### Important distinction

A Viper function is a pure logical expression.
A Viper method is a statement-level construct with a contract.

---

## 12. Lowered Control Flow

Generated methods often use a lowered control-flow style, for example:

```viper
goto lbl$ret$0
label lbl$ret$0
```

This is common in compiler-generated Viper.

### Why it appears

Structured source constructs such as:

- early returns
- branching
- loops
- expression-bodied methods

may be lowered into an explicit control-flow graph.

### Best practice when reading

Mentally reconstruct the structured source control flow and treat labels as a compilation artifact.

---

## 13. Common Statement Forms

### Assignment

```viper
x := y
```

### Assertion

```viper
assert P
```

Requires the verifier to prove `P`.

### Assumption via `inhale`

```viper
inhale P
```

Adds `P` to the current state.

### Requirement checking via method contracts

Method calls do not use a special `call` keyword. They are written as assignments to result variables, and Viper uses the callee's contract at the call site.

---

## 14. Contracts and Their Roles

Generated encodings rely heavily on contracts.

### `requires`

A caller obligation.
The caller must establish it before calling a method.

### `ensures`

A callee guarantee.
The caller may assume it after the call.

### Important modeling principle

Typing assumptions that callers are responsible for should normally appear in `requires`, not only as `inhale` inside the body.

If a generated method merely inhales a typing fact internally, then callers may be able to pass ill-typed values while still receiving a typed postcondition. That can make the encoding unsound.

---

## 15. Common Naming Conventions

Generated names are often mangled.

Typical prefixes:

- `df$...` for domain-level functions
- `rt` for runtime-related declarations
- `sp$...` for specification primitives
- `f$...` for translated source-level declarations
- `anon$...` for temporaries
- `lbl$...` for labels

### Best practice when documenting

Explain the naming scheme once, then switch to human-readable descriptions.

---

## 16. Typical Soundness Pitfalls

When reviewing or documenting a Viper encoding, pay special attention to the following.

### 16.1 Missing preconditions

A method that relies on a type assumption should typically declare it as a `requires` clause.

Potential problem pattern:

```viper
inhale isSubtype(typeOf(p), intType())
```

inside the method body, without a matching `requires`.

### 16.2 Weak postconditions

A method body may compute a very specific result, but the contract may only state its type.

This limits what callers can prove.

### 16.3 Over-powerful axioms

An incorrect axiom can make the whole encoding unsound. Axioms deserve especially careful review.

### 16.4 Mismatch between runtime type facts and value flow

If the encoding allows values to be copied freely while independently asserting incompatible runtime type facts, type safety may be lost.

---

## 17. Recommended Reading Strategy

When faced with a large generated encoding, use this order:

1. identify the runtime domain
2. identify the built-in type constructors
3. locate `typeOf`, `isSubtype`, and `nullable`
4. inspect boxing and unboxing laws
5. identify primitive operation specifications
6. read method and function contracts before their bodies
7. reconstruct the original source structure from generated temporaries and labels
8. check whether typing assumptions appear in `requires` or only as `inhale`

This approach keeps the most important semantic information in view.

---

## 18. Minimal Glossary

### Domain

An abstract logical namespace containing functions, axioms, and related declarations.

### Boxed value

A source-language value represented as a `Ref` in the encoding.

### Runtime type

An abstract logical value representing the type of a boxed runtime value.

### Specification primitive

An abstract function whose postconditions define the semantics of a built-in operator.

### Smart cast

A refinement step that turns nullable or union-like information into a more precise type fact after a check.

---

## 19. Example Summary Pattern

A concise way to summarize many generated Viper encodings is:

> The encoding uses an abstract runtime domain to model source-language types and subtyping, boxes all runtime values as `Ref`, connects primitive values to references through boxing and unboxing axioms, specifies built-in operators through abstract functions with postconditions, and lowers source-level functions and methods into Viper expressions or statements with generated temporaries and explicit control flow.

This is often the right one-paragraph overview for documentation.

---

## 20. Documentation Checklist

Use this checklist when documenting a new encoding:

- state what the runtime domain models
- list the built-in type constructors
- explain `typeOf`, `isSubtype`, and `nullable`
- explain boxing and unboxing
- summarize the key axioms
- explain the role of specification primitives
- distinguish translated functions from translated methods
- mention generated temporaries, labels, and gotos
- explain where typing assumptions live
- note any soundness risks or unusual modeling choices

---

## 21. Final Advice

The most important habit when documenting generated Viper is to separate:

- **semantic content**: types, values, contracts, axioms
- **proof-engineering artifacts**: triggers, temporaries, labels, lowered control flow

Both matter, but they matter for different reasons.

If the goal is understanding the program, start with semantics.
If the goal is debugging verification, then inspect the proof-engineering details next.