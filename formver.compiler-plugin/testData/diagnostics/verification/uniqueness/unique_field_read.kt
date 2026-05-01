// CHECK_UNIQUENESS
// RENDER_PREDICATES
// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

class X(var v: Int)

// Two reads of `x.v` separated by a local-val assignment should agree when `x` is unique:
// uniqueness rules out aliasing, so the linearizer can drop the conservative havoc on the
// second read.
@AlwaysVerify
fun <!VIPER_TEXT!>readMemberUnique<!>(@Unique x: X) {
    val k = x.v
    verify(k == x.v)
}

// A freshly-constructed unique local should also have stable reads of its var fields.
@AlwaysVerify
fun <!VIPER_TEXT!>constructAndRead<!>() {
    @Unique val x = X(42)
    val k = x.v
    verify(k == x.v)
}
