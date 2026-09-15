// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

class HeapCell(@Unique var value: Int)

@AlwaysVerify
fun <!VIPER_TEXT!>constructorReadControl<!>() {
    val cell = HeapCell(1)
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 1<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>directMutationControl<!>() {
    val cell = HeapCell(1)
    cell.value = 2
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 2<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>aliasObservesMutation<!>() {
    val cell = HeapCell(1)
    val alias = cell
    cell.value = 2
    verify(<!VIPER_VERIFICATION_ERROR!>alias.value == 2<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nullableAliasObservesMutation<!>() {
    val cell = HeapCell(1)
    val alias: HeapCell? = cell
    if (alias != null) {
        cell.value = 2
        verify(<!VIPER_VERIFICATION_ERROR!>alias.value == 2<!>)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>staleValueBoundary<!>() {
    val cell = HeapCell(1)
    val alias = cell
    cell.value = 2
    verify(<!VIPER_VERIFICATION_ERROR!>alias.value == 1<!>)
}

@Manual
class ManualHeapCell(@Unique var value: Int)

@AlwaysVerify
fun <!VIPER_TEXT!>manualConstructorReadControl<!>() {
    val cell = ManualHeapCell(1)
    unfold(UniquePred(cell))
    verify(cell.value == 1)
    fold(UniquePred(cell))
}

@AlwaysVerify
fun <!VIPER_TEXT!>manualAliasObservesMutation<!>() {
    val cell = ManualHeapCell(1)
    val alias = cell
    unfold(UniquePred(cell))
    cell.value = 2
    verify(alias.value == 2)
    fold(UniquePred(cell))
}

@AlwaysVerify
fun <!VIPER_TEXT!>manualNullableAliasObservesMutation<!>() {
    val cell = ManualHeapCell(1)
    val alias: ManualHeapCell? = cell
    unfold(UniquePred(cell))
    if (alias != null) {
        cell.value = 2
        verify(alias.value == 2)
    }
    fold(UniquePred(cell))
}
