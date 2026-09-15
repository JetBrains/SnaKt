// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

class MetamorphicCell(@Unique var value: Int)

fun <!VIPER_TEXT!>scalarPositiveControl<!>() {
    verify(1 + 1 == 2)
}

fun <!VIPER_TEXT!>scalarNegativeControl<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>1 + 1 == 3<!>)
}

fun <!VIPER_TEXT!>directMutation<!>(@Unique @Borrowed cell: MetamorphicCell) {
    preconditions { cell.value == 1 }
    cell.value = 2
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 2<!>)
}

fun <!VIPER_TEXT!>directMutationNegatedForm<!>(@Unique @Borrowed renamed: MetamorphicCell) {
    preconditions { renamed.value == 1 }
    renamed.value = 2
    verify(<!VIPER_VERIFICATION_ERROR!>!(renamed.value != 2)<!>)
}

fun <!VIPER_TEXT!>aliasedMutationConversion<!>(@Unique cell: MetamorphicCell) {
    val alias = cell
    alias.value = 3
}

fun <!VIPER_TEXT!>independentUpdatesLeftFirst<!>() {
    val left = MetamorphicCell(4)
    val right = MetamorphicCell(5)
    left.value = 6
    right.value = 7
}

fun <!VIPER_TEXT!>independentUpdatesRightFirst<!>() {
    val left = MetamorphicCell(4)
    val right = MetamorphicCell(5)
    right.value = 7
    left.value = 6
}

fun <!VIPER_TEXT!>nullableMutationDirect<!>(cell: @Unique MetamorphicCell?): Int {
    if (cell == null) return 0
    cell.value = 8
    return cell.value
}

fun <!VIPER_TEXT!>nullableMutationNegatedForm<!>(renamed: @Unique MetamorphicCell?): Int {
    if (!(renamed != null)) return 0
    renamed.value = 8
    return renamed.value
}
