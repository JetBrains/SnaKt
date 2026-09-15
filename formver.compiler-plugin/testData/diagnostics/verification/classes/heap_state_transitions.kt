// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

class TransitionCell(var value: Int)

class TransitionBox(
    @Unique var cell: TransitionCell,
    var optional: TransitionCell?,
)

@AlwaysVerify
fun <!VIPER_TEXT!>mutateAndRestore<!>() {
    val cell = TransitionCell(0)
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 0<!>)

    cell.value = 1
    verify(cell.value == 1)

    cell.value = 0
    verify(cell.value == 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>observeMutationThroughAlias<!>() {
    val cell = TransitionCell(10)
    val alias = cell
    verify(<!VIPER_VERIFICATION_ERROR!>alias.value == 10<!>)

    cell.value = 20
    verify(alias.value == 20)

    alias.value = 30
    verify(cell.value == 30)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nullableDetachAndReattach<!>() {
    val cell = TransitionCell(4)
    var reference: TransitionCell? = cell
    verify(reference != null)

    reference = null
    verify(<!SENSELESS_COMPARISON!>reference == null<!>)

    reference = cell
    verify(<!SENSELESS_COMPARISON!>reference != null<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>replaceNestedCellAndMutate<!>() {
    val first = TransitionCell(1)
    val box = TransitionBox(first, null)
    verify(<!VIPER_VERIFICATION_ERROR!>box.cell.value == 1<!>)
    verify(box.optional == null)

    val second = TransitionCell(2)
    box.cell = second
    verify(box.cell.value == 2)

    box.cell.value = 3
    verify(second.value == 3)

    box.optional = first
    verify(box.optional != null)
    verify(box.optional?.value == 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>rejectStaleValueAfterMutation<!>() {
    val cell = TransitionCell(7)
    cell.value = 8
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 7<!>)
}
