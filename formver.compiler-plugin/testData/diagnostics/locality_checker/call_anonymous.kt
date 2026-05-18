// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `call local-receiver lambda directly with local receiver`(x: @Borrowed Any, f: (@Borrowed Any).() -> Unit) {
    x.f()
}

fun `call local-receiver lambda directly with global receiver`(x: Any, f: (@Borrowed Any).() -> Unit) {
    x.f()
}

fun `call global-receiver lambda directly with local receiver`(x: @Borrowed Any, f: (Any).() -> Unit) {
    <!LOCALITY_MISMATCH!>x<!>.f()
}

fun `call global-argument lambda directly with global argument`(x: Any, f: (Any) -> Unit) {
    f(x)
}

fun `call local-argument lambda directly with local argument`(x: @Borrowed Any, f: (@Borrowed Any) -> Unit) {
    f(x)
}

fun `call global-argument lambda directly with local argument`(x: @Borrowed Any, f: (Any) -> Unit) {
    f(<!LOCALITY_MISMATCH!>x<!>)
}

fun `call local-local-argument lambda directly with local arguments`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (@Borrowed Any, @Borrowed Any) -> Unit
) {
    f(x, y)
}

fun `call global-local-argument lambda directly with local arguments`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (Any, @Borrowed Any) -> Unit
) {
    f(<!LOCALITY_MISMATCH!>x<!>, y)
}

fun `call local-global-argument lambda directly with local arguments`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (@Borrowed Any, Any) -> Unit
) {
    f(x, <!LOCALITY_MISMATCH!>y<!>)
}

fun `call local-receiver-local-argument lambda directly with local receiver and local argument`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (@Borrowed Any).(@Borrowed Any) -> Unit
) {
    x.f(y)
}

fun `call local-receiver-global-argument lambda directly with local receiver and local argument`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (@Borrowed Any).(Any) -> Unit
) {
    x.f(<!LOCALITY_MISMATCH!>y<!>)
}

fun `call global-receiver-local-argument lambda directly with local receiver and local argument`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (Any).(@Borrowed Any) -> Unit
) {
    <!LOCALITY_MISMATCH!>x<!>.f(y)
}

fun `call mixed-argument lambda expression with local argument`(x: @Borrowed Any, f: (Any) -> Unit, g: (@Borrowed Any) -> Unit) {
    (if (true) { f } else { g })(<!LOCALITY_MISMATCH!>x<!>)
}

fun `call mixed-argument lambda variable with local argument`(x: @Borrowed Any, f: (Any) -> Unit, g: (@Borrowed Any) -> Unit) {
    val h = if (true) { f } else { g }

    h(<!LOCALITY_MISMATCH!>x<!>)
}

fun `call mixed-global-local-argument lambda expression with local arguments`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (Any, @Borrowed Any) -> Unit,
    g: (@Borrowed Any, @Borrowed Any) -> Unit
) {
    (if (true) { f } else { g })(<!LOCALITY_MISMATCH!>x<!>, y)
}

fun `call mixed-global-local-argument lambda variable with local arguments`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (Any, @Borrowed Any) -> Unit,
    g: (@Borrowed Any, @Borrowed Any) -> Unit
) {
    val h = if (true) { f } else { g }

    h(<!LOCALITY_MISMATCH!>x<!>, y)
}

fun `call mixed-local-global-argument lambda expression with local arguments`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (@Borrowed Any, Any) -> Unit,
    g: (@Borrowed Any, @Borrowed Any) -> Unit
) {
    (if (true) { f } else { g })(x, <!LOCALITY_MISMATCH!>y<!>)
}

fun `call mixed-local-global-argument lambda variable with local arguments`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (@Borrowed Any, Any) -> Unit,
    g: (@Borrowed Any, @Borrowed Any) -> Unit
) {
    val h = if (true) { f } else { g }

    h(x, <!LOCALITY_MISMATCH!>y<!>)
}

fun `call mixed-global-global-argument lambda expression with local arguments`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (Any, @Borrowed Any) -> Unit,
    g: (@Borrowed Any, Any) -> Unit
) {
    (if (true) { f } else { g })(<!LOCALITY_MISMATCH!>x<!>, <!LOCALITY_MISMATCH!>y<!>)
}

fun `call mixed-global-global-argument lambda variable with local arguments`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (Any, @Borrowed Any) -> Unit,
    g: (@Borrowed Any, Any) -> Unit
) {
    val h = if (true) { f } else { g }

    h(<!LOCALITY_MISMATCH!>x<!>, <!LOCALITY_MISMATCH!>y<!>)
}

fun `call local-argument lambda expression with local argument`(x: @Borrowed Any, f: (@Borrowed Any) -> Unit, g: (@Borrowed Any) -> Unit) {
    (if (true) { f } else { g })(x)
}

fun `call local-argument lambda variable with local argument`(x: @Borrowed Any, f: (@Borrowed Any) -> Unit, g: (@Borrowed Any) -> Unit) {
    val h = if (true) { f } else { g }

    h(x)
}

fun `call local-argument lambda literal with local argument`(x: @Borrowed Any) {
    ({ x: @Borrowed Any ->
        var y = x
        Unit
    })(x)
}

fun `call local-argument lambda literal expression with local argument`(x: @Borrowed Any) {
    (if (true) { x: @Borrowed Any -> Unit } else { x: @Borrowed Any -> Unit })(x)
}

fun `call global-argument lambda literal expression with local argument`(x: @Borrowed Any) {
    (if (true) { x: @Borrowed Any -> Unit } else { x: Any -> Unit })(<!LOCALITY_MISMATCH!>x<!>)
}

fun `explicitly invoke local-argument lambda with local argument`(x: @Borrowed Any, f: (@Borrowed Any) -> Unit) {
    f.invoke(x)
}

fun `explicitly invoke global-argument lambda with local argument`(x: @Borrowed Any, f: (Any) -> Unit) {
    f.invoke(<!LOCALITY_MISMATCH!>x<!>)
}

fun `explicitly invoke mixed-global-local-argument lambda variable with local arguments`(
    x: @Borrowed Any,
    y: @Borrowed Any,
    f: (Any, @Borrowed Any) -> Unit,
    g: (@Borrowed Any, @Borrowed Any) -> Unit
) {
    val h = if (true) { f } else { g }

    h.invoke(<!LOCALITY_MISMATCH!>x<!>, y)
}
