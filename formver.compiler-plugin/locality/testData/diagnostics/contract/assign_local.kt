// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

val localFunction: (@Borrowed Any) -> Unit =
    { _: @Borrowed Any -> Unit }

val globalFunction: (Any) -> Unit =
    { _: Any -> Unit }

fun `assign global function to local function`() {
    var f: (@Borrowed Any) -> Unit

    f = <!LOCALITY_CONTRACT_MISMATCH!>globalFunction<!>
}

fun `assign global function to implicit local function`() {
    var f = localFunction

    f = <!LOCALITY_CONTRACT_MISMATCH!>globalFunction<!>
}

fun `assign local function to global function`() {
    var f: (Any) -> Unit

    f = localFunction
}

fun `assign implicit global function to explicit local function`() {
    val f = globalFunction

    val g: (@Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>f<!>
}

fun `assign global function to local function declaration`() {
    val f: (@Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>globalFunction<!>
}

fun `assign global function to local function variable`() {
    var f: (@Borrowed Any) -> Unit = localFunction
    f = <!LOCALITY_CONTRACT_MISMATCH!>globalFunction<!>
}

fun `assign global function with global argument to local function with local argument`() {
    var f: @Borrowed (@Borrowed Any) -> Unit

    f = <!LOCALITY_CONTRACT_MISMATCH!>globalFunction<!>
}

fun `assign global function with local argument to local function with local argument`() {
    var f: @Borrowed (@Borrowed Any) -> Unit

    f = localFunction
}

fun `assign local function with local argument to global function with local argument`(g: @Borrowed (@Borrowed Any) -> Unit) {
    var f: (@Borrowed Any) -> Unit

    // The locality contract matches, but the locality of the property itself doesn't.
    f = <!LOCALITY_MISMATCH!>g<!>
}

fun `assign local function with local argument to local function with global argument`(g: @Borrowed (@Borrowed Any) -> Unit) {
    var f: @Borrowed (Any) -> Unit

    f = g
}
