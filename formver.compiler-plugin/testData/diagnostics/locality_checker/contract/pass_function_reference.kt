// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun globalFunction(x: Any) {}

fun requireGlobalFunction(f: (Any) -> Unit) {}

fun localFunction(x: @Borrowed Any) {}

fun requireLocalFunction(f: (@Borrowed Any) -> Unit) {}

fun globalBinaryFunction(x: Any, y: Any) {}

fun requireGlobalBinaryFunction(f: (Any, Any) -> Unit) {}

fun localBinaryFunction(x: @Borrowed Any, y: @Borrowed Any) {}

fun requireLocalBinaryFunction(f: (@Borrowed Any, @Borrowed Any) -> Unit) {}

fun `pass globalFunction to requireGlobalFunction`() {
    requireGlobalFunction(::globalFunction)
}

fun `pass localFunction to requireGlobalFunction`() {
    requireGlobalFunction(::localFunction)
}

fun `pass globalFunction to requireLocalFunction`() {
    requireLocalFunction(<!LOCALITY_CONTRACT_MISMATCH!>::globalFunction<!>)
}

fun `pass localFunction to requireLocalFunction`() {
    requireLocalFunction(::localFunction)
}

fun `pass globalBinaryFunction to requireGlobalBinaryFunction`() {
    requireGlobalBinaryFunction(::globalBinaryFunction)
}

fun `pass localBinaryFunction to requireGlobalBinaryFunction`() {
    requireGlobalBinaryFunction(::localBinaryFunction)
}

fun `pass globalBinaryFunction to requireLocalBinaryFunction`() {
    requireLocalBinaryFunction(<!LOCALITY_CONTRACT_MISMATCH!>::globalBinaryFunction<!>)
}

fun `pass localBinaryFunction to requireLocalBinaryFunction`() {
    requireLocalBinaryFunction(::localBinaryFunction)
}
