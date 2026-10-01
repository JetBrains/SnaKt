// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

class HeapCell(var value: Int)

class ImmutableHeapCell(val value: Int)

class NullableHeapBox(@Unique var cell: HeapCell?)

@AlwaysVerify
fun <!VIPER_TEXT!>constructorAndSharedAliasControl<!>() {
    val cell = ImmutableHeapCell(1)
    val alias = cell
    verify(cell.value == alias.value)
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutationThroughUniqueParameterControl<!>(@Unique cell: HeapCell) {
    cell.value = 7
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 7<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>staleValueAfterMutationFails<!>(@Unique cell: HeapCell) {
    cell.value = 7
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 6<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nullableFieldBoundaryControl<!>(@Unique box: NullableHeapBox) {
    box.cell = null
    verify(<!VIPER_VERIFICATION_ERROR!>box.cell == null<!>)
}
