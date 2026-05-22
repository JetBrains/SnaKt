// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
    var w: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
}

fun `return shared subproperty`(a: B): Any {
    return a.y
}

fun `return borrowed subproperty`(a: @Borrowed B): Any {
    return a.y
}

fun `return unique subproperty`(a: @Unique B): Any {
    return a.y
}

fun `return unique-borrowed subproperty`(a: @Unique @Borrowed B): Any {
    return a.y
}
