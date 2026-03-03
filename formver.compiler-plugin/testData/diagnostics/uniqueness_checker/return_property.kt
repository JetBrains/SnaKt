// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

abstract class A {
    @Unique abstract var x: Any
    @Unique abstract var w: Any
}

abstract class B {
    @Unique abstract var y: A
}

fun `return shared subproperty`(a: B): Any {
    return a.y
}

fun `return borrowed subproperty`(@Borrowed a: B): Any {
    <!UNIQUENESS_VIOLATION!>return a.y<!>
}

fun `return unique subproperty`(@Unique a: B): Any {
    return a.y
}

fun `return unique-borrowed subproperty`(@Unique @Borrowed a: B): Any {
    <!UNIQUENESS_VIOLATION!>return a.y<!>
}
