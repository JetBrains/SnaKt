// FULL_JDK
// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

class ModelCell(var value: Int)

class AccessorCell(initial: Int) {
    var value: Int = initial
        get() = field
        set(next) {
            field = next
        }
}

@AlwaysVerify
fun <!VIPER_TEXT!>positiveControl<!>() {
    verify(0 == 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>constructorNegativeOracle<!>() {
    val cell = ModelCell(-1)

    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == -1<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>constructorZeroOracle<!>() {
    val cell = ModelCell(0)

    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 0<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>constructorPositiveOracle<!>() {
    val cell = ModelCell(1)

    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 1<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>directMutationZeroOracle<!>() {
    val cell = ModelCell(-1)

    cell.value = 0
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 0<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>directMutationPositiveOracle<!>() {
    val cell = ModelCell(0)

    cell.value = 1
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 1<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>aliasMutationOracle<!>() {
    val original = ModelCell(-1)
    val alias = original

    verify(original === alias)
    alias.value = 1
    verify(<!VIPER_VERIFICATION_ERROR!>original.value == 1<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nullableAliasOracle<!>() {
    val original = ModelCell(0)
    val alias: ModelCell? = original

    if (alias != null) {
        verify(original === alias)
        alias.value = -1
        verify(<!VIPER_VERIFICATION_ERROR!>original.value == -1<!>)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>uniqueOwnershipOracle<!>(@Unique cell: ModelCell) {
    verify(cell === cell)
    cell.value = 1
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 1<!>)
}

@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>accessorMutationOracle<!>() {
    val original = AccessorCell(-1)
    val alias = original

    alias.value = 1
    verify(<!PURITY_VIOLATION!>original.value == 1<!>, <!PURITY_VIOLATION!>alias.value == 1<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>negativeMutationControl<!>() {
    val cell = ModelCell(-1)

    cell.value = 1
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == -1<!>)
}
