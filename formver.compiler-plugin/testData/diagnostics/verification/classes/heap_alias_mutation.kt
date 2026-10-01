// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

class MutableCell(var value: Int)

@AlwaysVerify
fun <!VIPER_TEXT!>aliasIdentityControl<!>(@Unique cell: MutableCell) {
    val alias = cell
    verify(alias === cell)
}

@AlwaysVerify
fun <!VIPER_TEXT!>constructorAndDirectMutation<!>() {
    val cell = MutableCell(1)
    cell.value = 2
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 2<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutationThroughAlias<!>(@Unique cell: MutableCell) {
    val alias = cell
    alias.value = 2
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 2<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>staleValueAfterAliasMutation<!>(@Unique cell: MutableCell) {
    cell.value = 1
    val alias = cell
    alias.value = 2
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 1<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nullableAliasMutation<!>(@Unique cell: MutableCell?) {
    if (cell != null) {
        val alias: MutableCell? = cell
        alias?.value = 3
        verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 3<!>)
    }
}

class AccessorCell(initial: Int) {
    var value: Int = initial
        get() = field + 1
        set(newValue) {
            field = newValue + 1
        }
}

@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>customAccessorBoundary<!>(@Unique cell: AccessorCell) {
    cell.value = 4
    verify(<!PURITY_VIOLATION!>cell.value == 6<!>)
}
