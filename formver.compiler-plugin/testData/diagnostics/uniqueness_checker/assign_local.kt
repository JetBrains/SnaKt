// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun nondet(): Boolean {
    return false
}

fun consume(@Unique x: Any) {}

// Var assignments

fun `assign shared`(x: Any) {
    var y: Any

    y = x

    consume(<!UNIQUENESS_VIOLATION!>y<!>)
}

fun `assign borrowed`(@Borrowed x: Any) {
    var y: Any

    y = x

    consume(<!UNIQUENESS_VIOLATION!>y<!>)
}

fun `assign unique`(@Unique x: Any) {
    var y: Any

    y = x

    consume(y)
}

fun `assign unique-borrowed`(@Unique @Borrowed x: Any) {
    var y: Any

    y = x

    consume(<!UNIQUENESS_VIOLATION!>y<!>)
}

// Var declarations

fun `assign shared in declaration`(x: Any) {
    var y = x

    consume(<!UNIQUENESS_VIOLATION!>y<!>)
}

fun `assign borrowed in declaration`(@Borrowed x: Any) {
    var y = x

    consume(<!UNIQUENESS_VIOLATION!>y<!>)
}

fun `assign unique in declaration`(@Unique x: Any) {
    var y = x

    consume(y)
}

fun `assign unique-borrowed in declaration`(@Unique @Borrowed x: Any) {
    var y = x

    consume(<!UNIQUENESS_VIOLATION!>y<!>)
}

// Assignment chaining

fun `chain shared assignments`(x: Any) {
    var y: Any
    var z: Any

    y = x
    z = y

    consume(<!UNIQUENESS_VIOLATION!>z<!>)
}

fun `chain borrowed assignments`(@Borrowed x: Any) {
    var y: Any
    var z: Any

    y = x
    z = y

    consume(<!UNIQUENESS_VIOLATION!>z<!>)
}

fun `chain unique assignments`(@Unique x: Any) {
    var y: Any
    var z: Any

    y = x
    z = y

    consume(z)
}

fun `chain unique-borrowed assignments`(@Unique @Borrowed x: Any) {
    var y: Any
    var z: Any

    y = x
    z = y

    consume(<!UNIQUENESS_VIOLATION!>z<!>)
}

// Conditional assignments

fun `assign unique or shared`(@Unique x: Any, y: Any) {
    var z: Any;

    if (nondet()) {
        z = x
    } else {
        z = y
    }

    consume(<!UNIQUENESS_VIOLATION!>z<!>)
}

// Looping assignments

fun `assign unique to shared in loop`(@Unique x: Any, y: Any) {
    var z: Any = y;

    while (nondet()) {
        z = x
    }

    consume(<!UNIQUENESS_VIOLATION!>z<!>)
}
