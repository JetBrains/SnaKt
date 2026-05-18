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

fun `assign mixed function if-expression to local function`(
    f: (Any) -> Unit,
    g: (@Borrowed Any) -> Unit
) {
    val h: (@Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>if (true) f else g<!>
}

fun `assign mixed function when-expression to local function`(
    f: (Any, @Borrowed Any) -> Unit,
    g: (@Borrowed Any, @Borrowed Any) -> Unit
) {
    val h: (@Borrowed Any, @Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>when {
        true -> f
        else -> g
    }<!>
}

fun `assign mixed function try-expression to local function`(
    f: (Any) -> Unit,
    g: (@Borrowed Any) -> Unit
) {
    val h: (@Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>try {
        f
    } catch (_: Throwable) {
        g
    }<!>
}

fun `assign mixed function nested control-flow to local function`(
    f: (Any) -> Unit,
    g: (@Borrowed Any) -> Unit
) {
    val h: (@Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>if (true) {
        when {
            true -> f
            else -> g
        }
    } else {
        g
    }<!>
}

fun `assign global function to local function declaration`() {
    val f: (@Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>globalFunction<!>
}

fun `assign global function to local function variable`() {
    var f: (@Borrowed Any) -> Unit = localFunction
    f = <!LOCALITY_CONTRACT_MISMATCH!>globalFunction<!>
}
