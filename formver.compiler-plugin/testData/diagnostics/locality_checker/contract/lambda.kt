// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `pass global function as local-function argument`(
    f: ((@Borrowed Any) -> Unit) -> Unit,
    g: (Any) -> Unit
) {
    f(<!LOCALITY_CONTRACT_MISMATCH!>g<!>)
}

fun `pass local function as global-function argument`(
    f: ((Any) -> Unit) -> Unit,
    g: (@Borrowed Any) -> Unit
) {
    f(g)
}

fun `pass global function as local-function argument to lambda variable`(
    f: ((@Borrowed Any) -> Unit) -> Unit,
    g: (Any) -> Unit
) {
    val h = f

    h(<!LOCALITY_CONTRACT_MISMATCH!>g<!>)
}

fun `pass global function as second local-function argument`(
    f: ((@Borrowed Any) -> Unit, (@Borrowed Any) -> Unit) -> Unit,
    g: (@Borrowed Any) -> Unit,
    h: (Any) -> Unit
) {
    f(g, <!LOCALITY_CONTRACT_MISMATCH!>h<!>)
}

fun `pass local function as local argument to anonymous function`(
    g: @Borrowed (Any) -> Unit
) {
    (fun(f: @Borrowed (Any) -> Unit) {})(g)
}

fun `pass global function as local-function argument to anonymous function`(
    g: (Any) -> Unit
) {
    (fun(f: (@Borrowed Any) -> Unit) {})(<!LOCALITY_CONTRACT_MISMATCH!>g<!>)
}

fun `pass global function as local-function receiver`(
    f: ((@Borrowed Any) -> Unit).() -> Unit,
    g: (Any) -> Unit
) {
    <!LOCALITY_CONTRACT_MISMATCH!>g<!>.f()
}

fun `pass local function as unified local function argument`(
    f: ((@Borrowed Any) -> Unit) -> Unit,
    g: ((Any) -> Unit) -> Unit
) {
    val h = if (false) { f } else { g }

    h({x: @Borrowed Any -> Unit})
}

fun `pass global function as unified local function argument`(
    f: ((@Borrowed Any) -> Unit) -> Unit,
    g: ((Any) -> Unit) -> Unit
) {
    val h = if (false) { f } else { g }

    h({x: Any -> Unit})
}

fun `pass global function as unified global function argument`(
    f: ((Any) -> Unit) -> Unit,
    g: ((Any) -> Unit) -> Unit
) {
    val h = if (false) { f } else { g }

    h({x: Any -> Unit})
}

fun `pass global nested function as local higher-order function argument`(
    f: (((@Borrowed Any) -> Unit) -> Unit) -> Unit
) {
    f(<!LOCALITY_CONTRACT_MISMATCH!>fun(g: (Any) -> Unit) { g(Any()) }<!>)
}

fun `pass global nested function as unified local higher-order function argument`(
    f: (((@Borrowed Any) -> Unit) -> Unit) -> Unit,
    g: (((Any) -> Unit) -> Unit) -> Unit
) {
    val h = if (false) { f } else { g }

    h(fun(k: (Any) -> Unit) { k(Any()) })
}

fun `explicitly invoke local-function argument with global function`(
    f: ((@Borrowed Any) -> Unit) -> Unit,
    g: (Any) -> Unit
) {
    f.invoke(<!LOCALITY_CONTRACT_MISMATCH!>g<!>)
}

fun `explicitly invoke unified local-function argument with global function`(
    f: ((@Borrowed Any) -> Unit) -> Unit,
    g: ((Any) -> Unit) -> Unit
) {
    val h = if (false) { f } else { g }

    h.invoke({x: Any -> Unit})
}

fun `explicitly invoke local higher-order function argument with global nested function`(
    f: (((@Borrowed Any) -> Unit) -> Unit) -> Unit
) {
    f.invoke(<!LOCALITY_CONTRACT_MISMATCH!>fun(g: (Any) -> Unit) { g(Any()) }<!>)
}
