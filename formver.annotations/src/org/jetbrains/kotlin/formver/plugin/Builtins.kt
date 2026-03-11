/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

private class FormverFunctionCalledInRuntimeException(offendingFunction: String) :
    RuntimeException("Function `$offendingFunction` should never be called in runtime.")

/**
 * Asserts that each of the given boolean predicates holds at the current program point.
 *
 * This function is a no-op at runtime; the SnaKt plugin intercepts it during compilation and
 * translates each argument to a Viper `assert` statement. All arguments must be pure expressions
 * (no side effects).
 *
 * ```kotlin
 * @AlwaysVerify
 * fun example(x: Int) {
 *     verify(x >= 0, x < 100)  // both predicates are asserted
 * }
 * ```
 *
 * @param predicates One or more boolean expressions to assert. Each is checked independently.
 */
fun verify(@Suppress("UNUSED_PARAMETER") vararg predicates: Boolean) = Unit

/**
 * Logical implication operator for use in specifications.
 *
 * `a implies b` is equivalent to `!a || b`. This is a no-op at runtime.
 * Primarily used inside `preconditions`, `postconditions`, `loopInvariants`, and `forAll` blocks.
 *
 * ```kotlin
 * postconditions<Int> { result ->
 *     (result > 0) implies (result >= 1)
 * }
 * ```
 */
infix fun Boolean.implies(other: Boolean) = !this || other

/**
 * Declares loop invariants for the enclosing `while` loop.
 *
 * Place this call at the top of a `while` loop body. Each boolean statement in the lambda block
 * is an invariant that must hold:
 * - Before the loop starts (established by the code before the loop)
 * - At the end of every iteration (re-established by the loop body)
 * - After the loop exits (available as a postcondition)
 *
 * This function is a no-op at runtime.
 *
 * ```kotlin
 * var i = 0
 * while (i < n) {
 *     loopInvariants {
 *         i >= 0
 *         i <= n
 *     }
 *     i++
 * }
 * ```
 */
fun loopInvariants(@Suppress("UNUSED_PARAMETER") body: InvariantBuilder.() -> Unit) = Unit

/**
 * Declares preconditions for the enclosing function.
 *
 * Each boolean statement in the lambda block is a precondition that callers must satisfy.
 * Inside the function body the verifier assumes all preconditions hold. A function that calls
 * this function must prove that all preconditions are satisfied at the call site.
 *
 * This function is a no-op at runtime.
 *
 * ```kotlin
 * @AlwaysVerify
 * fun divide(a: Int, b: Int): Int {
 *     preconditions { b != 0 }
 *     return a / b
 * }
 * ```
 */
fun preconditions(@Suppress("UNUSED_PARAMETER") body: InvariantBuilder.() -> Unit) = Unit

/**
 * Declares postconditions for the enclosing function.
 *
 * Each boolean statement in the lambda block is a property that the function guarantees on return.
 * The lambda parameter names the return value and may be used in the postcondition expressions.
 * `it` is available as shorthand when only one lambda parameter is used.
 *
 * Postconditions are available as preconditions at call sites: callers may assume the postconditions
 * hold after the function returns.
 *
 * This function is a no-op at runtime.
 *
 * ```kotlin
 * @AlwaysVerify
 * fun abs(x: Int): Int {
 *     postconditions<Int> { result ->
 *         result >= 0
 *         result == x || result == -x
 *     }
 *     return if (x >= 0) x else -x
 * }
 * ```
 *
 * @param T The return type of the enclosing function.
 */
fun <T> postconditions(@Suppress("UNUSED_PARAMETER") body: InvariantBuilder.(T) -> Unit) = Unit

/**
 * Receiver class for lambda blocks passed to [loopInvariants], [preconditions], and [postconditions].
 *
 * Instances of this class are never created at runtime. The SnaKt plugin intercepts the lambda
 * body at compile time and translates each expression to the corresponding Viper specification.
 */
class InvariantBuilder {
    /**
     * Universal quantification over all values of type [T].
     *
     * Asserts that the predicate in the lambda body holds for every possible value of type [T].
     * May only be used inside [preconditions], [postconditions], or [loopInvariants] blocks.
     *
     * This function throws at runtime if called (it should never be called outside of a
     * specification block).
     *
     * ```kotlin
     * postconditions<Int> { res ->
     *     forAll<Int> { x ->
     *         x * x >= 0        // true for all integers
     *         x * x >= res
     *     }
     * }
     * ```
     *
     * Use [triggers] inside the lambda to guide the SMT solver when automatic instantiation fails.
     *
     * @param T The type to quantify over.
     * @param body A predicate over a value of type [T]; each expression is an independent assertion.
     */
    fun <T> forAll(@Suppress("UNUSED_PARAMETER") body: (T) -> Unit): Boolean =
        throw FormverFunctionCalledInRuntimeException("forAll")

    /**
     * Provides explicit SMT solver trigger expressions for the enclosing [forAll] quantifier.
     *
     * Triggers guide the SMT solver in deciding when to instantiate the quantifier. Without triggers
     * the solver uses heuristics; providing triggers can fix verification failures caused by the
     * solver not instantiating a quantifier it needs.
     *
     * This function must be called at the top of a [forAll] lambda, before the predicate expressions.
     * It throws at runtime if called outside a specification block.
     *
     * ```kotlin
     * forAll<Int> { x ->
     *     triggers(x * x)     // instantiate whenever x*x appears in the proof context
     *     x * x >= 0
     * }
     * ```
     *
     * @param expressions One or more terms that trigger quantifier instantiation.
     */
    fun triggers(@Suppress("UNUSED_PARAMETER") vararg expressions: Any?): Unit =
        throw FormverFunctionCalledInRuntimeException("triggers")
}
