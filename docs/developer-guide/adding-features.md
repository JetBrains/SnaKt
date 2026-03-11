# Adding Features

This page is a practical guide for the most common types of code changes. Each recipe lists the files to touch and the steps to follow.

---

## 1. Supporting a new Kotlin expression

### Where to look

Start by finding how a similar existing expression is handled. The main dispatch lives in `StmtConversionVisitor` (inside `StmtConverter.kt`):

```
formver.compiler-plugin/core/src/…/core/conversion/StmtConverter.kt
```

`StmtConversionVisitor` is a `FirVisitor<ExpEmbedding, StmtConversionContext>` and has one `visit*` method per FIR node type. Find the visitor method for an expression similar to the one you want to add and use it as a template.

### Steps

**1. Add a visit method to `StmtConversionVisitor`.**

For a FIR node type `FirMyExpression`, add:

```kotlin
override fun visitMyExpression(
    expression: FirMyExpression,
    data: StmtConversionContext,
): ExpEmbedding {
    // Convert sub-expressions recursively, then build the embedding.
    val subExpr = data.convert(expression.subExpression)
    return MyExpressionEmbedding(subExpr)
}
```

**2. Add a new `ExpEmbedding` subtype if needed.**

If the expression requires a new intermediate representation node, add it in:

```
formver.compiler-plugin/core/src/…/core/embeddings/expression/
```

Implement the `ExpEmbedding` interface:

```kotlin
class MyExpressionEmbedding(val sub: ExpEmbedding) : ExpEmbedding {
    override fun toViperUnusedResult(ctx: LinearizationContext) { ... }
    override fun toViperStoringIn(variable: VariableEmbedding, ctx: LinearizationContext) { ... }
    override fun toViperBuiltinType(ctx: LinearizationContext): Exp { ... }
}
```

Also add a `visitMyExpressionEmbedding` method to `ExpVisitor` and implement it in all visitor implementations:

- `PurityChecker` (in `core/purity/`): declares whether the embedding is pure.
- `DebugExpEmbeddingPrinter` (in `core/embeddings/expression/debug/`): provides a debug string representation.
- Any other `ExpVisitor` subclasses that need to handle the new node.

**3. Implement linearization.**

In the `toViperUnusedResult`, `toViperStoringIn`, and `toViperBuiltinType` methods, emit the appropriate Viper AST nodes using the `LinearizationContext` API:

- Call `ctx.addStatement { ... }` to emit a Viper statement.
- Call `ctx.store(lhs, rhs)` to emit an assignment.
- Call `ctx.addBranch(condition, thenBranch, elseBranch, type, result)` to emit a conditional.
- Return a `Viper.Exp` from `toViperBuiltinType` for pure expressions.

**4. Add a test case.**

Add a `.kt` file to `testData/diagnostics/conversion/` or the appropriate `verification/` subdirectory. See [Testing Guide](testing.md) for the full procedure.

---

## 2. Fixing a verification error report

### Where to look

Error mapping happens in:

```
formver.compiler-plugin/plugin/src/…/plugin/compiler/reporting/FormattedError.kt
```

The `VerificationError.formatUserFriendly()` function dispatches on the `SourceRole` tag recovered from the Silicon error.

### Steps

**1. Identify the error.**

Run the failing test with `errorStyle("both")` to see both the user-friendly message and the raw Silicon error. The Silicon `id` (e.g. `"postcondition.violated:assertion.false"`) tells you what kind of failure occurred.

**2. Add or extend a `SourceRole` tag.**

If the failing assertion comes from a new kind of Kotlin construct, add a new `SourceRole` subtype in:

```
formver.compiler-plugin/core/src/…/core/embeddings/SourceRole.kt
```

Then tag the relevant `ExpEmbedding` during conversion (or linearization) by setting `info = sourceRole.asInfo` on the Viper AST node.

**3. Add the mapping in `FormattedError.kt`.**

In `VerificationError.formatUserFriendly()`, add a new branch:

```kotlin
is SourceRole.MyNewRole -> MyNewRoleError(this, sourceRole)
```

Implement `MyNewRoleError` as a `FormattedError` that calls the appropriate `PluginErrors.*` diagnostic.

**4. Declare the diagnostic if needed.**

If you need a new diagnostic, add it to `PluginErrors.kt`:

```kotlin
val MY_NEW_ERROR by warning1<PsiElement, String>()
```

And add its message to the diagnostic renderer factory.

**5. Add a test.**

Add a test in `testData/diagnostics/verification/` that exercises the new error message. Use `-Pupdate` to generate the expected output, then inspect the `.fir.diag.txt` file.

---

## 3. Adding a new annotation or built-in function

### Steps

**1. Declare the annotation in `formver.annotations`.**

Add the annotation class to:

```
formver.annotations/src/…/plugin/Annotations.kt
```

or the built-in function to `Builtins.kt`.

**2. Detect the annotation during conversion.**

In `MethodConverter.kt` or `StmtConverter.kt`, check for the annotation on the FIR symbol:

```kotlin
if (symbol.hasAnnotation(myAnnotationId, session)) {
    // Handle the annotation.
}
```

Pre-compute the `ClassId` for the annotation as a constant so the lookup is efficient.

**3. Emit the appropriate Viper construct.**

Depending on what the annotation means:

- For a new precondition/postcondition: add it to `embedFullSignature`'s `getPreconditions()` or `getPostconditions()` list.
- For a new statement modifier: emit a `Stmt.Inhale`, `Stmt.Exhale`, `Stmt.Assert`, `Stmt.Fold`, or `Stmt.Unfold` in the function body.
- For a new `ExpEmbedding` attribute: set a flag on the embedding and use it during linearization.

**4. Add tests.**

Add tests covering both the expected happy path and common error cases.

---

## 4. Adding a new stdlib special function

### When to use this

Use this approach when a Kotlin standard library function should be replaced with a Viper built-in operator or expression rather than a method call.

### Steps

**1. Determine if the function is always-special or partially-special.**

- *Always-special*: the Viper translation is the same regardless of argument types (e.g. `Int.plus`). Use `FullySpecialKotlinFunctionBuilder`.
- *Partially-special*: the translation depends on argument types (e.g. `String.plus(Any?)`). Subclass `PartiallySpecialKotlinFunction`.

**2. For an always-special function, add an entry in `SpecialKotlinFunctions.kt`.**

Inside the `buildFullySpecialFunctions { ... }` block:

```kotlin
withCallableType(myFunctionType) {
    addFunction(
        packageName = listOf("kotlin"),
        className = "MyClass",
        name = "myFunction",
    ) { args, ctx ->
        MyViperExpressionEmbedding(args[0], args[1])
    }
}
```

The function is automatically registered in `ProgramConverter.methods` at initialization.

**3. For a partially-special function, implement `PartiallySpecialKotlinFunction`.**

Override `insertCall(args, ctx)` to dispatch on argument types and either produce a special embedding or call `baseEmbedding!!.insertCall(args, ctx)` for the fallback.

Register the instance in `PartiallySpecialKotlinFunctions.generateAllByName()`.

**4. Add tests.**

See [Testing Guide](testing.md).

---

## 5. Supporting a new class type

### Steps

**1. Add the class embedding to `ProgramConverter.embedTypeWithBuilder`.**

In the `TypeBuilder.embedTypeWithBuilder(type: ConeKotlinType)` function, add a branch for the new type:

```kotlin
type.isMyNewClass -> myNewClass()
```

If the class has heap fields, ensure it goes through `embedClass` so its fields and predicates are registered.

**2. Register fields and predicates.**

If the class needs backing fields, implement them as `FieldEmbedding` instances and add them to the class's `ClassEmbeddingDetails` via `initFields`. The Viper field declarations are then included in the `Program.fields` list automatically.

Access predicates (`sharedPredicate` and `uniquePredicate`) are generated automatically from the field list in `ClassEmbeddingDetails`.

**3. Add to `RuntimeTypeDomain` if the class needs type axioms.**

If the class participates in the Kotlin type hierarchy and you need Viper axioms encoding subtype relationships, ensure `embedClass` is called for the class and its supertypes. The `RuntimeTypeDomain` is built from `classes.values` and includes all embedded classes automatically.

**4. Add special properties if needed.**

If the class has properties that require special Viper treatment (like `String.length`), add them to `SpecialProperties.byCallableId`.

**5. Add tests.**

Add tests in `testData/diagnostics/verification/` covering the new class's properties.

---

## General tips

- **Read the KDoc**: the source files have detailed KDoc comments explaining design decisions and edge cases. Read these before making changes.
- **Use `@DumpExpEmbeddings`**: add this annotation to a test function to print the intermediate `ExpEmbedding` tree during conversion. This is the fastest way to see what is going wrong.
- **Check the Viper output**: use `logLevel("full_viper_dump")` to see the generated Viper code. Compare it against what you expect from the [Viper tutorial](http://viper.ethz.ch/tutorial/).
- **Run the full test suite** before opening a pull request — a change in one area often has unexpected effects elsewhere.
