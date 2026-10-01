// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.verify

class MutableCell(var value: Int)
class ConstructedCell(val value: Int)

@AlwaysVerify
fun <!VIPER_TEXT!>constructorAliasPositiveControl<!>() {
    val original = ConstructedCell(7)
    val alias = original
    verify(alias.value == 7)
}

@AlwaysVerify
fun <!VIPER_TEXT!>constructorAliasMutatedControl<!>() {
    val original = ConstructedCell(8)
    val alias = original
    verify(<!VIPER_VERIFICATION_ERROR!>alias.value == 7<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutableAliasAssignmentBoundary<!>(@Unique @Borrowed cell: MutableCell) {
    postconditions<Unit> {
        cell.value == 7
    }

    val alias = cell
    alias.value = 7
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 7<!>)
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>mutableAliasContractNegative<!>(@Unique @Borrowed cell: MutableCell) {
    postconditions<Unit> {
        cell.value == 7
    }

    val alias = cell
    alias.value = 8
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>nullableAliasAssignmentBoundary<!>(@Unique @Borrowed cell: MutableCell?) {
    postconditions<Unit> {
        cell == null || cell.value == 11
    }

    val alias = cell
    if (alias != null) {
        alias.value = 11
    }
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>nullableAliasContractNegative<!>(@Unique @Borrowed cell: MutableCell?) {
    postconditions<Unit> {
        cell == null || cell.value == 11
    }

    val alias = cell
    if (alias != null) {
        alias.value = 12
    }
}<!>
