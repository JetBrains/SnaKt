// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
}

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

fun share(a: Any) {}

// Extension functions with annotated receivers

fun @Unique A.consumeReceiver() {}

fun @Borrowed A.borrowReceiver() {}

fun A.shareReceiver() {}

// Calling extension consumes the receiver

fun `consume via extension function on unique`(a: @Unique A) {
    a.consumeReceiver()
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
}

// TODO: This diagnostic is over-zealous. `borrowReceiver` has a `@Borrowed A` extension receiver
// and should not consume `a`, but the analyzer eagerly moves the receiver in
// `visitQualifiedAccessNode` (when the receiver expression `a` is read into the call) and
// never re-initializes it. `visitFunctionCallExitNode` only re-initializes parameters listed
// in `resolvedArgumentMapping`, which does NOT include the extension receiver.
fun `borrow via extension function on unique`(a: @Unique A) {
    a.borrowReceiver()
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
}

fun `share via extension function on unique`(a: @Unique A) {
    a.shareReceiver()
    // TODO: Same gap as `borrow via extension function on unique`. Additionally,
    // `shareReceiver` has no uniqueness/locality annotation on its receiver -- a unique
    // value escaping into a shared receiver should be flagged separately.
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
}

// Using `this` in an extension body. Because the receiver isn't seeded into the function's
// initial uniqueness state (only value parameters are), `this` is also reported as `shared`
// and gets moved on every access.

fun (@Unique A).`consume this in receiver body`() {
    // TODO: `this` is the @Unique extension receiver; both consumes should be considered.
    // The first one fires as 'shared' (actual) vs 'unique' (expected) because the receiver
    // parameter isn't in the initial state.
    consume(this)
    consume(<!UNIQUENESS_MISMATCH!>this<!>)
}

fun (@Unique A).`borrow this in receiver body`() {
    borrow(this)
    // TODO: Same receiver-not-in-initial-state gap as above.
    consume(this)
}

// Implicit vs explicit `this`

fun (@Unique A).`use implicit then explicit this`() {
    borrow(this)
    borrow(this@`use implicit then explicit this`)
    // TODO: Same gap.
    consume(this)
}

// Mixing receiver and argument in same call

fun (@Borrowed A).borrowSelfAndArg(other: @Borrowed A) {}

fun `pass same value as receiver and arg`(a: @Unique A) {
    // TODO: Determine whether this error makes sense. Since both the receiver and the argument are borrowed shouldn't
    // it be possible to pass the same value as both?
    a.borrowSelfAndArg(a)
    consume(a)
}
