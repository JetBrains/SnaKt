// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    @Unique var x = Object()
    @Unique var w = Object()
}

class B {
    @Unique var y = A()
}

fun `return shared subproperty`(a: B): Any {
    return a.y
}

fun `return borrowed subproperty`(@Borrowed a: B): Any {
    return <!UNIQUENESS_VIOLATION!>a.y<!>
}

fun `return unique subproperty`(@Unique a: B): Any {
    return a.y
}

fun `return unique-borrowed subproperty`(@Unique @Borrowed a: B): Any {
    return <!UNIQUENESS_VIOLATION!>a.y<!>
}
