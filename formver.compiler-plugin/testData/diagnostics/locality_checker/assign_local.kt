// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `assign global to local`(x: Any) {
    @Borrowed var y: Any

    y = x
}

fun `assign local to global`(@Borrowed x: Any) {
    var y: Any

    y = <!LOCALITY_VIOLATION!>x<!>
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

fun `assign local to local in lambda`(@Borrowed x: Any) {
    { y: Any ->
        @Borrowed var z: Any = <!LOCALITY_VIOLATION!>x<!>
    }
}

fun `assign local if-expression to local`(@Borrowed x: Any) {
    @Borrowed var z: Any = if (false) { x } else { Any() }
}

fun `assign local if-expression to global`(@Borrowed x: Any) {
    var z: Any = <!LOCALITY_VIOLATION!>if (false) { x } else { Any() }<!>
}

fun `assign local if-expression to local in lambda`(@Borrowed x: Any) {
    { y: Any ->
        @Borrowed var z: Any = <!LOCALITY_VIOLATION!>if (false) { x } else { Any() }<!>
    }
}

fun `assign local if-expression to local in loop`(@Borrowed x: Any) {
    @Borrowed var z: Any = Any()

    while (true) {
        z = if (false) { x } else { Any() }
    }
}

fun `assign local when-expression to local`(@Borrowed x: Any) {
    @Borrowed var z: Any = when {
        false -> x
        else -> Any()
    }
}

fun `assign local when-expression to global`(@Borrowed x: Any) {
    var z: Any = <!LOCALITY_VIOLATION!>when {
        false -> x
        else -> Any()
    }<!>
}

fun `assign local when-expression to global in loop`(@Borrowed x: Any) {
    var z: Any = Any()

    while (true) {
        z = <!LOCALITY_VIOLATION!>when {
            false -> x
            else -> Any()
        }<!>
    }
}

fun `assign local try-expression to global`(@Borrowed x: Any) {
    var z: Any = <!LOCALITY_VIOLATION!>try {
        x
    } catch (_: Throwable) {
        Any()
    }<!>
}

fun `assign local try-expression to global in loop`(@Borrowed x: Any) {
    var z: Any = Any()

    while (true) {
        z = <!LOCALITY_VIOLATION!>try {
            x
        } catch (_: Throwable) {
            Any()
        }<!>
    }
}

fun `assign local cast-expression to global`(@Borrowed x: Any) {
    var z: String = <!LOCALITY_VIOLATION!>x as String<!>
}

fun `assign local not-null-expression to global`(@Borrowed x: Any?) {
    var z: Any = <!LOCALITY_VIOLATION!>x!!<!>
}

fun `assign local nested control-flow to global in loop`(@Borrowed x: Any) {
    var z: Any = Any()

    while (true) {
        z = <!LOCALITY_VIOLATION!>if (false) {
            when {
                false -> x
                else -> Any()
            }
        } else {
            Any()
        }<!>

        z = <!LOCALITY_VIOLATION!>try {
            if (false) { x } else { Any() }
        } catch (_: Throwable) {
            Any()
        }<!>
    }
}

fun `assign local nested control-flow to local in loop`(@Borrowed x: Any) {
    @Borrowed var z: Any = Any()

    while (true) {
        z = when {
            false -> if (false) { x } else { Any() }
            else -> x
        }
    }
}

fun `assign local nested control-flow to global in lambda loop`(@Borrowed x: Any) {
    {
        var z: Any = Any()

        while (true) {
            z = <!LOCALITY_VIOLATION!>when {
                false -> try { x } catch (_: Throwable) { Any() }
                else -> Any()
            }<!>

            z = <!LOCALITY_VIOLATION!>if (false) {
                x
            } else {
                try { Any() } catch (_: Throwable) { x }
            }<!>
        }
    }
}
