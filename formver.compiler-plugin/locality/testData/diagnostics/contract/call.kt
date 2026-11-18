// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed


fun requireLocalFunction(f: (@Borrowed Any) -> Unit) {}

fun requireGlobalFunction(f: (Any) -> Unit) {}

fun requireLocalFunctionWithLocalArgument(f: @Borrowed (@Borrowed Any) -> Unit) {}

fun requireLocalFunctionWithGlobalArgument(f: @Borrowed (Any) -> Unit) {}

fun requireTwoLocalFunctions(f: (@Borrowed Any) -> Unit, g: (@Borrowed Any) -> Unit) {}

fun requireLocalLocalFunction(f: (@Borrowed Any, @Borrowed Any) -> Unit) {}

fun requireLocalReceiverFunction(f: (@Borrowed Any).() -> Unit) {}

fun ((@Borrowed Any) -> Unit).requireLocalFunctionReceiver() {}

fun produceLocalFunction(): (@Borrowed Any) -> Unit =
    { _: @Borrowed Any -> Unit }

fun produceGlobalFunction(): (Any) -> Unit =
    { _: Any -> Unit }

fun produceGlobalLocalFunction(): (Any, @Borrowed Any) -> Unit =
    { _: Any, _: @Borrowed Any -> Unit }

fun produceGlobalReceiverFunction(): (Any).() -> Unit =
    { Unit }

fun `pass global function as local function argument`() {
    requireLocalFunction(<!LOCALITY_CONTRACT_MISMATCH!>produceGlobalFunction()<!>)
}

fun `pass global function as named local function argument`() {
    requireLocalFunction(f = <!LOCALITY_CONTRACT_MISMATCH!>produceGlobalFunction()<!>)
}

fun `pass global function as second local function argument`() {
    requireTwoLocalFunctions(produceLocalFunction(), <!LOCALITY_CONTRACT_MISMATCH!>produceGlobalFunction()<!>)
}

fun `pass global-local function as local-local function argument`() {
    requireLocalLocalFunction(<!LOCALITY_CONTRACT_MISMATCH!>produceGlobalLocalFunction()<!>)
}

fun `pass local function as global function argument`() {
    requireGlobalFunction(produceLocalFunction())
}

fun `pass global receiver function as local receiver function`() {
    requireLocalReceiverFunction(<!LOCALITY_CONTRACT_MISMATCH!>produceGlobalReceiverFunction()<!>)
}

fun `pass global function as local function receiver`() {
    <!LOCALITY_CONTRACT_MISMATCH!>produceGlobalFunction()<!>.requireLocalFunctionReceiver()
}

fun `pass local value to local function with local argument`(f: @Borrowed (@Borrowed Any) -> Unit, x: @Borrowed Any) {
    f(x)
}

fun `pass local value to local function with global argument`(f: @Borrowed (Any) -> Unit, x: @Borrowed Any) {
    f(<!LOCALITY_MISMATCH!>x<!>)
}

fun `pass global function with global argument as local function with local argument`() {
    requireLocalFunctionWithLocalArgument(<!LOCALITY_CONTRACT_MISMATCH!>produceGlobalFunction()<!>)
}

fun `pass global function with local argument as local function with local argument`() {
    requireLocalFunctionWithLocalArgument(produceLocalFunction())
}

fun `pass local function with global argument as local function with local argument`(f: @Borrowed (Any) -> Unit) {
    requireLocalFunctionWithLocalArgument(<!LOCALITY_CONTRACT_MISMATCH!>f<!>)
}

fun `pass local function with local argument as local function with local argument`(f: @Borrowed (@Borrowed Any) -> Unit) {
    requireLocalFunctionWithLocalArgument(f)
}

fun `pass local function with local argument as local function with global argument`(f: @Borrowed (@Borrowed Any) -> Unit) {
    requireLocalFunctionWithGlobalArgument(f)
}

fun `pass local function with local argument as global function with local argument`(f: @Borrowed (@Borrowed Any) -> Unit) {
    // The locality contract matches, but the locality of the property itself doesn't.
    requireLocalFunction(<!LOCALITY_MISMATCH!>f<!>)
}

fun `pass local function with local argument as global function with global argument`(f: @Borrowed (@Borrowed Any) -> Unit) {
    requireGlobalFunction(<!LOCALITY_MISMATCH!>f<!>)
}

fun `pass local lambda with local argument as local function with local argument`(x: @Borrowed Any) {
    requireLocalFunctionWithLocalArgument { y: @Borrowed Any -> val z: @Borrowed Any = x }
}

fun `pass local lambda with global argument as local function with local argument`(x: @Borrowed Any) {
    requireLocalFunctionWithLocalArgument(<!LOCALITY_CONTRACT_MISMATCH!>{ y: Any -> val z: @Borrowed Any = x }<!>)
}

fun `pass local lambda with local argument as global function with local argument`(x: @Borrowed Any) {
    requireLocalFunction(<!LOCALITY_MISMATCH!>{ y: @Borrowed Any -> val z: @Borrowed Any = x }<!>)
}
