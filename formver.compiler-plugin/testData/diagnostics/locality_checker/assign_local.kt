// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `assign global to local`(x: Any) {
    var y: @Borrowed Any

    y = x
}

fun `assign local to global`(x: @Borrowed Any) {
    var y: Any

    y = <!LOCALITY_MISMATCH!>x<!>
}

fun `assign local to local`(x: @Borrowed Any) {
    var y: @Borrowed Any

    y = x
}

fun `assign local then global to local`(x: @Borrowed Any, y: Any) {
    var z: @Borrowed Any

    z = x
    z = y
}

fun `assign local to local in loop`(x: @Borrowed Any) {
    var z: @Borrowed Any

    while (true) {
        z = x
    }
}

fun `assign local to local in lambda`(x: @Borrowed Any) {
    { y: Any ->
        var z: @Borrowed Any = <!INVALID_LOCALITY_CAPTURE!>x<!>
    }
}

fun `assign local if-expression to local`(x: @Borrowed Any) {
    var z: @Borrowed Any = if (false) { x } else { Any() }
}

fun `assign local if-expression to global`(x: @Borrowed Any) {
    var z: Any = <!LOCALITY_MISMATCH!>if (false) { x } else { Any() }<!>
}

fun `assign local if-expression to local in lambda`(x: @Borrowed Any) {
    { y: Any ->
        var z: @Borrowed Any = if (false) { <!INVALID_LOCALITY_CAPTURE!>x<!> } else { Any() }
    }
}

fun `assign local if-expression to local in loop`(x: @Borrowed Any) {
    var z: @Borrowed Any = Any()

    while (true) {
        z = if (false) { x } else { Any() }
    }
}

fun `assign local when-expression to local`(x: @Borrowed Any) {
    var z: @Borrowed Any = when {
        false -> x
        else -> Any()
    }
}

fun `assign local when-expression to global`(x: @Borrowed Any) {
    var z: Any = <!LOCALITY_MISMATCH!>when {
        false -> x
        else -> Any()
    }<!>
}

fun `assign local when-expression to global in loop`(x: @Borrowed Any) {
    var z: Any = Any()

    while (true) {
        z = <!LOCALITY_MISMATCH!>when {
            false -> x
            else -> Any()
        }<!>
    }
}

fun `assign local try-expression to global`(x: @Borrowed Any) {
    var z: Any = <!LOCALITY_MISMATCH!>try {
        x
    } catch (_: Throwable) {
        Any()
    }<!>
}

fun `assign local try-expression to global in loop`(x: @Borrowed Any) {
    var z: Any = Any()

    while (true) {
        z = <!LOCALITY_MISMATCH!>try {
            x
        } catch (_: Throwable) {
            Any()
        }<!>
    }
}

fun `assign local cast-expression to global`(x: @Borrowed Any) {
    var z: String = <!LOCALITY_MISMATCH!>x as String<!>
}

fun `assign local safe-cast-expression to global`(x: @Borrowed Any) {
    var z: String? = <!LOCALITY_MISMATCH!>x as? String<!>
}

fun `assign local not-null-expression to global`(x: @Borrowed Any?) {
    var z: Any = <!LOCALITY_MISMATCH!>x!!<!>
}

fun `assign local nested control-flow to global in loop`(x: @Borrowed Any) {
    var z: Any = Any()

    while (true) {
        z = <!LOCALITY_MISMATCH!>if (false) {
            when {
                false -> x
                else -> Any()
            }
        } else {
            Any()
        }<!>

        z = <!LOCALITY_MISMATCH!>try {
            if (false) { x } else { Any() }
        } catch (_: Throwable) {
            Any()
        }<!>
    }
}

fun `assign local nested control-flow to local in loop`(x: @Borrowed Any) {
    var z: @Borrowed Any = Any()

    while (true) {
        z = when {
            false -> if (false) { x } else { Any() }
            else -> x
        }
    }
}

fun `assign local nested control-flow to global in lambda loop`(x: @Borrowed Any) {
    {
        var z: Any = Any()

        while (true) {
            z = <!LOCALITY_MISMATCH!>when {
                false -> try { <!INVALID_LOCALITY_CAPTURE!>x<!> } catch (_: Throwable) { Any() }
                else -> Any()
            }<!>

            z = <!LOCALITY_MISMATCH!>if (false) {
                <!INVALID_LOCALITY_CAPTURE!>x<!>
            } else {
                try { Any() } catch (_: Throwable) { <!INVALID_LOCALITY_CAPTURE!>x<!> }
            }<!>
        }
    }
}

fun `assign merged local owners to global`(x: @Borrowed Any) {
    { y: @Borrowed Any ->
        var z: Any = <!LOCALITY_MISMATCH!>if (false) { <!INVALID_LOCALITY_CAPTURE!>x<!> } else { y }<!>
    }
}

fun nondet() = false

fun `assign local to implicit local`(x: @Borrowed Any) {
    var a = x as A
}

class A

fun `assign local cast to implicit global`(x: @Borrowed Any, y: A) {
    var a = y

    a = <!LOCALITY_MISMATCH!>x as A<!>
}

fun `assign implicit local to explicit global`(x: @Borrowed Any) {
    var a = x as A

    var b: Any = <!LOCALITY_MISMATCH!>a<!>
}

fun `assign global to implicit local`(x: @Borrowed Any, y: A) {
    var a = x as A

    a = y
}

fun `assign local smartcast expression to local`(x: @Borrowed Any) {
    var a: @Borrowed A
    var b: @Borrowed A = A()

    a = when (x) {
        is A -> x
        else -> b
    }
}

fun `assign global smartcast expression to local`(x: Any) {
    var a: @Borrowed A
    var b: @Borrowed A = A()

    a = when (x) {
        is A -> x
        else -> A()
    }
}

fun `assign local smartcast expression to global`(x: @Borrowed Any) {
    var a: A
    var b: @Borrowed A = A()

    a = <!LOCALITY_MISMATCH!>when (x) {
        is A -> x
        else -> b
    }<!>
}
