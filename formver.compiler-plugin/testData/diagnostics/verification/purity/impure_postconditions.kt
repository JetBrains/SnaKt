// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

fun <!VIPER_TEXT!>impureTopLevelPredicate<!>(value: Int): Boolean = value > 0

class PredicateHolder {
    fun <!VIPER_TEXT!>impureMemberPredicate<!>(): Boolean = true
}

<!PURITY_VIOLATION!>@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>impureTopLevelCallInPostcondition<!>(value: Int) {
    postconditions<Unit> {
        impureTopLevelPredicate(value)
    }
}<!>

<!PURITY_VIOLATION!>@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>impureMemberCallInPostcondition<!>(holder: PredicateHolder) {
    postconditions<Unit> {
        holder.impureMemberPredicate()
    }
}<!>
