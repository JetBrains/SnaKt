// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

fun f1(@Unique x: Any): Any {
    var y: Any

    y = x

    return y
}

fun f2(@Unique x: Any): Any {
    var y: Any
    var z: Any

    y = x
    z = y

    return z
}

fun f3(@Unique x: Any): Any {
    var y = x

    return y
}

fun nondet(): Boolean {
    return false
}

fun f4(@Unique x: Any, y: Any): Any {
    var z: Any;

    if (nondet()) {
        z = x
    } else {
        z = y
    }

    return z
}

fun f5(@Unique x: Any, y: Any): Any {
    var z: Any = y;

    while (nondet()) {
        z = x
    }

    <!UNIQUENESS_VIOLATION!>return z<!>
}

fun f6(@Unique x: Any, @Unique y: Any): Any {
    var z = x
    z = y

    return z
}

fun f7(@Unique x: Any): Any {
    var y = x
    var z = y

    <!UNIQUENESS_VIOLATION!>return y<!>
}

fun f8(@Unique a: Any, @Unique b: Any): Any {
    var x = a
    var y = b
    var tmp = x
    x = y
    y = tmp

    return x
}

fun f9(@Unique x: Any, @Unique @Borrowed y: Any): Any {
    var z: Any

    if (nondet()) {
        z = x
    } else {
        z = x
    }

    return z
}

fun f10(@Unique @Borrowed a: Any, b: Any): Any {
    var x = a
    x = b

    return x
}

fun f11(@Unique x: Any): Any {
    var y = x

    <!UNIQUENESS_VIOLATION!>return x<!>
}
