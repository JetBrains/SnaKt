// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

fun f1(@Unique @Borrowed x: Any): Any {
    var y: Any

    y = x

    return y
}

fun f2(@Unique @Borrowed x: Any): Any {
    var y: Any
    var z: Any

    y = x
    z = y

    return z
}

fun f3(@Unique @Borrowed x: Any): Any {
    var y = x

    return y
}

fun nondet(): Boolean {
    return false
}

fun f4(@Unique @Borrowed x: Any, y: Any): Any {
    var z: Any;

    if (nondet()) {
        z = x
    } else {
        z = y
    }

    return z
}

fun f5(@Unique @Borrowed x: Any, y: Any): Any {
    var z: Any = y;

    while (nondet()) {
        z = x
    }

    <!UNIQUENESS_VIOLATION!>return z<!>
}
