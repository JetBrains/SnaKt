// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A(
    var x: Any
)

fun `assign local to global property`(x: A, @Borrowed y: Any) {
    x.x = <!LOCALITY_VIOLATION!>y<!>
}

fun `assign local to local property`(@Borrowed x: A, @Borrowed y: Any) {
    x.x = <!LOCALITY_VIOLATION!>y<!>
}

fun `assign global property access to local`(x: A) {
    @Borrowed var z: Any = x.x
}

fun `assign local property access to local`(@Borrowed x: A) {
    @Borrowed var z: Any = x.x
}

fun `assign global safe-call expression to local`(x: A?) {
    @Borrowed var z: Any? = x?.x
}

fun `assign local safe-call expression to local`(@Borrowed x: A?) {
    @Borrowed var z: Any? = x?.x
}

fun `assign local safe-call expression to global`(@Borrowed x: A?) {
    var z: Any? = x?.x
}

class B(
    var x: Any
)

fun `assign global property to local property`(@Borrowed x: B, y: B) {
    x.x = y.x
}

fun `assign local property to global property`(x: B, @Borrowed y: B) {
    x.x = y.x
}

fun `assign local property to local property`(@Borrowed x: B, @Borrowed y: B) {
    x.x = y.x
}

fun `assign local property if-expression to global`(@Borrowed x: A, y: A) {
    var z: Any = if (false) { x.x } else { y.x }
}

fun `assign local property when-expression to global in loop`(@Borrowed x: A, y: A) {
    var z: Any = Any()

    while (true) {
        z = when {
            false -> x.x
            else -> y.x
        }
    }
}

fun `assign global property when-expression to global in loop`(x: A, y: A) {
    var z: Any = Any()

    while (true) {
        z = when {
            false -> x.x
            else -> y.x
        }
    }
}

fun `assign local property try-expression to global`(@Borrowed x: A, y: A) {
    var z: Any = try {
        x.x
    } catch (_: Throwable) {
        y.x
    }
}

fun `assign local property if-expression to global property in loop`(x: B, @Borrowed y: B, z: B) {
    while (true) {
        x.x = if (false) { y.x } else { z.x }
    }
}

fun `assign local property when-expression to local property in loop`(@Borrowed x: B, y: B, @Borrowed z: B) {
    while (true) {
        x.x = when {
            false -> y.x
            else -> z.x
        }
    }
}

fun `assign local property try-expression to global property`(x: B, @Borrowed y: B, z: B) {
    x.x = try {
        y.x
    } catch (_: Throwable) {
        z.x
    }
}

fun `assign local property try-expression to local property`(@Borrowed x: B, @Borrowed y: B, z: B) {
    x.x = try {
        y.x
    } catch (_: Throwable) {
        z.x
    }
}
