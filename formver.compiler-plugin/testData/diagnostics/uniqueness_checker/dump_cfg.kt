// UNIQUE_CHECK_ONLY
// DUMP_UNIQUENESS_CFG

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A(
    @Unique var first: B,
    @Unique var second: B,
)


class B()

fun <!UNIQUENESS_CFG!>nonDet<!>() : Boolean {
    return true
}

fun <!UNIQUENESS_CFG!>test<!>(@Unique a: A) : A{

    if (nonDet()) {
        var x = a.first
    } else {
        @Unique var y = a.second
    }

    return a

}
