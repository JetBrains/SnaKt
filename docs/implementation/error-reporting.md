# Error Reporting

SnaKt translates low-level Silicon verification failures into user-friendly Kotlin compiler diagnostics. This page describes how errors flow from Silicon to the IDE or build output.

---

## The verification pipeline and where errors come from

After the Kotlin function has been converted to a Viper `Program` and handed to Silicon, Silicon returns `VerificationError` objects. Each `VerificationError` contains:

- `id`: a Silicon error-class identifier (e.g. `"postcondition.violated:assertion.false"`).
- `msg`: a raw human-readable message from Silicon.
- `position`: the Viper source position (a `Position` wrapping Silicon's position type).
- `locationNode`: the Viper AST node where the error occurred.
- `unverifiableProposition`: the Viper assertion expression that could not be proved.

These Viper-level objects are meaningful to developers but not to users, who wrote Kotlin code and have no visibility into the generated Viper.

---

## SourceRole: tagging embeddings with Kotlin context

During conversion, every `ExpEmbedding` node that corresponds to a semantically significant Kotlin construct is tagged with a `SourceRole`. The tag is carried inside a Viper `Info.Wrapped(sourceRole)` node attached to the Viper AST node produced by linearization.

When Silicon reports a verification failure, the error's `locationNode` or `unverifiableProposition` may carry an `Info` payload. `VerifierErrorInterpreter` unwraps this payload to recover the original `SourceRole` and produce a matching diagnostic.

### Lookup strategy

```
VerificationError.lookupSourceRole():
  1. Try locationNode.getInfoOrNull<SourceRole>()
  2. If null, try unverifiableProposition.getInfoOrNull<SourceRole>()
```

For example, `PreconditionInCallFalse` errors have the call site as the offending node, but the failing precondition is in `unverifiableProposition`. Checking both ensures the role is found in either case.

---

## SourceRole subtypes

`SourceRole` is a sealed interface in `formver.compiler-plugin/core/src/…/core/embeddings/SourceRole.kt`.

### ReturnsEffect

Marks assertions that encode Kotlin `contract { returns(X) }` effects.

| Subtype | Meaning |
|---|---|
| `ReturnsEffect.Wildcard` | The function returned (any value) |
| `ReturnsEffect.Bool(bool)` | The function returned `true` or `false` |
| `ReturnsEffect.Null(negated)` | The function returned `null` or non-null |

### ConditionalEffect

Marks assertions that encode `contract { returns(X) implies condition }` conditional effects.

`ConditionalEffect(effect: ReturnsEffect, condition: Condition)` pairs a return effect with its triggering condition.

### Condition

Represents the condition side of a `ConditionalEffect`:

| Subtype | Meaning |
|---|---|
| `Condition.IsNull(variable, negated)` | Variable is (not) null |
| `Condition.IsType(variable, type, negated)` | Variable is (not) an instance of `type` |
| `Condition.Constant(literal)` | Constant `true`/`false` |
| `Condition.Conjunction(lhs, rhs)` | `lhs && rhs` |
| `Condition.Disjunction(lhs, rhs)` | `lhs \|\| rhs` |
| `Condition.Negation(arg)` | `!arg` |

`FirSymbolHolder(firSymbol)` also implements `Condition` and `SourceRole`, carrying a reference to the original FIR symbol (used when reporting list-related errors to include the list's name).

### ListElementAccessCheck

Marks Viper assertions that guard list element accesses:

| Subtype | Meaning |
|---|---|
| `AccessCheckType.LESS_THAN_ZERO` | Index is non-negative |
| `AccessCheckType.GREATER_THAN_LIST_SIZE` | Index is less than list size |

### SubListCreation

Marks Viper assertions that guard sublist range parameters:

| Subtype | Meaning |
|---|---|
| `SubListCreation.CheckNegativeIndices` | Start/end indices are non-negative |
| `SubListCreation.CheckInSize` | End index does not exceed list size |

---

## How errors are mapped: VerifierErrorInterpreter

`VerifierErrorInterpreter` is the bridge between Silicon errors and Kotlin diagnostics. The `formatUserFriendly()` extension on `VerificationError` performs the mapping:

```kotlin
fun VerificationError.formatUserFriendly(): FormattedError? =
    when (val sourceRole = lookupSourceRole()) {
        is SourceRole.ReturnsEffect      -> ReturnsEffectError(sourceRole)
        is SourceRole.ConditionalEffect  -> ConditionalEffectError(sourceRole)
        is SourceRole.ListElementAccessCheck -> IndexOutOfBoundError(this, sourceRole)
        is SourceRole.SubListCreation    -> InvalidSubListRangeError(this, sourceRole)
        else -> null  // fall back to DefaultError
    }
```

If `lookupSourceRole()` returns null (no `SourceRole` tag found), `null` is returned and `DefaultError` is used instead.

---

## FormattedError

`FormattedError` is a sealed interface with a single method:

```kotlin
sealed interface FormattedError {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun report(source: KtSourceElement?)
}
```

Each implementation calls the appropriate `PluginErrors.*` diagnostic:

| FormattedError class | Diagnostic emitted |
|---|---|
| `DefaultError` | `VIPER_VERIFICATION_ERROR` |
| `ReturnsEffectError` | `UNEXPECTED_RETURNED_VALUE` |
| `ConditionalEffectError` | `CONDITIONAL_EFFECT_ERROR` |
| `IndexOutOfBoundError` | `POSSIBLE_INDEX_OUT_OF_BOUND` |
| `InvalidSubListRangeError` | `INVALID_SUBLIST_RANGE` |

---

## The diagnostics

All diagnostics are declared in `PluginErrors` in `formver.compiler-plugin/plugin/src/…/plugin/compiler/PluginErrors.kt`.

### Warnings (verification failures)

| Diagnostic | Meaning |
|---|---|
| `VIPER_VERIFICATION_ERROR` | Generic Silicon verification error; raw Viper message shown |
| `UNEXPECTED_RETURNED_VALUE` | A `returns(X)` contract was violated: the function returned an unexpected value |
| `CONDITIONAL_EFFECT_ERROR` | A `returns(X) implies condition` contract was violated |
| `POSSIBLE_INDEX_OUT_OF_BOUND` | A list index access may be out of bounds |
| `INVALID_SUBLIST_RANGE` | A sublist range may be invalid |

### Errors (analysis failures)

| Diagnostic | Meaning |
|---|---|
| `PURITY_VIOLATION` | A `@Pure` function body contains an impure operation (side effect, method call) |
| `UNIQUENESS_VIOLATION` | An ownership/uniqueness constraint was violated |

### Plugin bugs

| Diagnostic | Meaning |
|---|---|
| `INTERNAL_ERROR` | A known internal error with a source location (thrown as `SnaktInternalException`) |
| `MINOR_INTERNAL_ERROR` | A non-fatal issue collected during conversion (e.g. an unsupported type substituted with `Unit`) |

### Informational

| Diagnostic | Meaning |
|---|---|
| `VIPER_TEXT` | The generated Viper program text (shown when `logLevel` is not `ONLY_WARNINGS`) |
| `VIPER_FILE` | URI of the `.formver/<name>.vpr` dump file |
| `EXP_EMBEDDING` | Debug dump of the intermediate `ExpEmbedding` tree (shown when `@DumpExpEmbeddings` is present) |

---

## ErrorStyle: controlling what is shown

`ErrorStyle` (in `formver.common`) controls which representation of a verification error is reported:

| Value | Effect |
|---|---|
| `USER_FRIENDLY` | Show only the user-friendly diagnostic (default) |
| `ORIGINAL_VIPER` | Show only the raw Silicon error message |
| `BOTH` | Show both the user-friendly diagnostic and the raw Silicon error |

The choice is applied in `VerificationError.formatByErrorStyle`:

```kotlin
fun VerificationError.formatByErrorStyle(errorStyle: ErrorStyle): List<FormattedError> = when (errorStyle) {
    ErrorStyle.USER_FRIENDLY -> listOf(formatUserFriendly() ?: DefaultError(this))
    ErrorStyle.ORIGINAL_VIPER -> listOf(DefaultError(this))
    ErrorStyle.BOTH -> listOfNotNull(formatUserFriendly(), DefaultError(this))
}
```

When `BOTH` is selected and no user-friendly mapping exists, only `DefaultError` is shown (no duplicate raw error).

To enable `BOTH` in your `build.gradle.kts`:

```kotlin
formver {
    errorStyle("both")
}
```

---

## Key source files

| File | Role |
|---|---|
| `core/embeddings/SourceRole.kt` | Sealed hierarchy of source-role tags |
| `plugin/compiler/reporting/FormattedError.kt` | Sealed hierarchy of formatted error types |
| `plugin/compiler/reporting/VerifierErrorInterpreter.kt` | Maps `VerificationError` → `FormattedError` |
| `plugin/compiler/reporting/VerifierErrorInterpreter.kt` | `reportVerifierError` top-level function |
| `plugin/compiler/PluginErrors.kt` | All diagnostic declarations |
| `viper/errors/VerificationError.kt` | Wraps Silicon failure results |
| `viper/errors/ConsistencyError.kt` | Wraps Silver consistency errors |
