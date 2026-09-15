// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

class MutableCell(@Unique var value: Int)

class CellHolder(@Unique var cell: MutableCell?) {
    var observed: Int
        get() = cell?.value ?: -1
        set(newValue) {
            cell?.value = newValue
        }
}

@AlwaysVerify
fun <!VIPER_TEXT!>aliasIdentityPositiveControl<!>() {
    val cell = MutableCell(17)
    val alias = cell
    verify(alias == cell)
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutationThroughAlias<!>(@Unique @Borrowed cell: MutableCell) {
    val alias = cell
    alias.value = 19
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 19<!>)
}

// Same heap operations with an inert primitive local between alias creation and mutation.
@AlwaysVerify
fun <!VIPER_TEXT!>mutationThroughAliasPerturbed<!>(@Unique @Borrowed cell: MutableCell) {
    val alias = cell
    val harmless = 0
    alias.value = 19 + harmless
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 19<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nullableAliasMutation<!>(@Unique @Borrowed holder: CellHolder) {
    val cell = holder.cell
    if (cell != null) {
        val alias = cell
        alias.value = 23
        verify(<!VIPER_VERIFICATION_ERROR!>holder.cell?.value == 23<!>)
    }
}

@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>setterMutationObservedByGetter<!>(@Unique @Borrowed holder: CellHolder) {
    if (holder.cell != null) {
        holder.observed = 29
        verify(<!PURITY_VIOLATION!>holder.observed == 29<!>)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>staleValueNegativeControl<!>(@Unique @Borrowed cell: MutableCell) {
    val alias = cell
    alias.value = 31
    verify(<!VIPER_VERIFICATION_ERROR!>cell.value == 30<!>)
}
