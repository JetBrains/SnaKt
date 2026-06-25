// LOCALITY_CHECK_ONLY
// LANGUAGE: +ContextParameters

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

fun nondet(): Boolean = false

fun borrow(x: @Borrowed A) {}

fun useBorrowed(f: @Borrowed () -> Unit) {
    //...
}

fun useGlobalAndBorrowed(g: () -> Unit, f: @Borrowed () -> Unit) {
    //...
}

fun (@Borrowed () -> Unit).callTwice() {
    this()
    this()
}

fun (() -> Unit).callTwiceGlobally() {
    this()
    this()
}

fun useContextFunction(f: @Borrowed context(A) () -> Unit) {
    //...
}

context(c: @Borrowed () -> Unit)
fun runFromContext() {
    c()
}

fun `capture local from global lambda capturing`(x: @Borrowed Any) {
    {
        var y = <!INVALID_LOCALITY_CAPTURE!>x<!>
    }
}

fun `capture local from local receiver lambda capturing`(x: @Borrowed Any) {
    {
        var y = x
    }.callTwice()
}

fun `capture local from nested global lambda`(x: @Borrowed Any) {
    {
        {
            var y = <!INVALID_LOCALITY_CAPTURE!>x<!>
        }
    }
}

fun `capture local from nested local lambda`(x: @Borrowed Any) {
    val f: @Borrowed () -> Unit = {
        var y = x
    }
}

fun `capture local from doubly nested local lambda`(x: @Borrowed Any) {
    val f: @Borrowed () -> Unit = {
        val g: @Borrowed () -> Unit = {
            var y = x
        }
    }
}

fun `capture local from triply nested local lambda`(x: @Borrowed Any) {
    val f: @Borrowed () -> Unit = {
        val g: @Borrowed () -> Unit = {
            val h: @Borrowed () -> Unit = {
                val y: @Borrowed Any = x
            }
        }
    }
}

fun `capturing a borrowed local from a local lambda is allowed`(x: @Borrowed Any) {
    val borrowX: @Borrowed () -> Unit = {
        val y: @Borrowed Any = x
    }

    borrowX()
}

fun `capturing a borrowed local from a local lambda as an argument is allowed`(x: @Borrowed Any) {
    useBorrowed {
        val y: @Borrowed Any = x
    }
}

fun `capture local property into local lambda`() {
    val z: @Borrowed Any = Any()

    val f: @Borrowed () -> Unit = {
        val y: @Borrowed Any = z
    }
}

fun `capture value declared in enclosing local lambda`() {
    val f: @Borrowed () -> Unit = {
        val z: @Borrowed Any = Any()

        val g: @Borrowed () -> Unit = {
            val y: @Borrowed Any = z
        }
    }
}

fun `capture into local lambda passed to borrowed slot`(x: @Borrowed Any) {
    useGlobalAndBorrowed({ }) {
        val y: @Borrowed Any = x
    }
}

fun `capture inside control flow in local lambda body`(x: @Borrowed Any) {
    val f: @Borrowed () -> Unit = {
        val y: @Borrowed Any = if (nondet()) x else x
    }
}

fun `capture into local lambda bound through if`(x: @Borrowed Any) {
    val f: @Borrowed () -> Unit = if (nondet()) {
        { val y: @Borrowed Any = x }
    } else {
        { }
    }
}

fun `capture into global lambda bound through if`(x: @Borrowed Any) {
    val f: () -> Unit = if (nondet()) {
        { val y = <!INVALID_LOCALITY_CAPTURE!>x<!> }
    } else {
        { }
    }
}

fun `capture into local lambda bound through when initializer`(x: @Borrowed Any) {
    val f: @Borrowed () -> Unit = when {
        nondet() -> { { val y: @Borrowed Any = x } }
        else -> { { } }
    }
}

fun `capture into local lambda bound through try as argument`(x: @Borrowed Any) {
    useBorrowed(
        try {
            { val y: @Borrowed Any = x }
        } catch (_: Throwable) {
            { }
        }
    )
}

fun `capture into local lambda bound through when as argument`(x: @Borrowed Any) {
    useBorrowed(
        when {
            nondet() -> { { val y: @Borrowed Any = x } }
            else -> { { } }
        }
    )
}

fun `capture into local lambda bound through cast as argument`(x: @Borrowed Any) {
    useBorrowed({ val y: @Borrowed Any = x } <!USELESS_CAST!>as () -> Unit<!>)
}

fun `capture into borrowed lambda used as receiver`(x: @Borrowed A) {
    { val y: @Borrowed A = x }.callTwice()
}

fun `capture into global lambda used as receiver`(x: @Borrowed A) {
    { val y: @Borrowed A = <!INVALID_LOCALITY_CAPTURE!>x<!> }.callTwiceGlobally()
}

fun `capture into local lambda with receiver type`(x: @Borrowed A) {
    val f: @Borrowed A.() -> Unit = {
        val y: @Borrowed A = x
    }
}

fun `capture into local lambda with context type`(x: @Borrowed A) {
    val f: @Borrowed context(A) () -> Unit = {
        val y: @Borrowed A = x
    }
}

fun `capture into borrowed context lambda passed as argument`(x: @Borrowed A) {
    useContextFunction {
        val y: @Borrowed A = x
    }
}

fun `provide capturing lambda as borrowed context via with`(x: @Borrowed A) {
    with({ val y: @Borrowed A = <!INVALID_LOCALITY_CAPTURE!>x<!> }) {
        runFromContext()
    }
}

fun `resolve captured local owner from nested lambda`(x: @Borrowed A) {
    val x: @Borrowed A = x

    run {
        borrow(<!INVALID_LOCALITY_CAPTURE!>x<!>)
    }

    borrow(x)
}

fun `resolve shadowing local's owner first`(x: @Borrowed A) {
    val x: @Borrowed A = x

    run {
        val x: @Borrowed A = A()
        borrow(x)
    }

    borrow(x)
}

fun `assign local to local in lambda`(x: @Borrowed Any) {
    { y: Any ->
        var z: @Borrowed Any = <!INVALID_LOCALITY_CAPTURE!>x<!>
    }
}

fun `assign local if-expression to local in lambda`(x: @Borrowed Any) {
    { y: Any ->
        var z: @Borrowed Any = if (false) { <!INVALID_LOCALITY_CAPTURE!>x<!> } else { Any() }
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

fun `pass outer local as local argument in lambda`(x: @Borrowed A) {
    {
        borrow(<!INVALID_LOCALITY_CAPTURE!>x<!>)
    }
}

fun `return local value from lambda body`(x: @Borrowed Any) {
    run<Any> {
        <!INVALID_LOCALITY_CAPTURE, LOCALITY_MISMATCH!>x<!>
    }
}

fun `throw local value from lambda`(x: @Borrowed Throwable) {
    run {
        throw <!INVALID_LOCALITY_CAPTURE, LOCALITY_MISMATCH!>x<!>
    }
}
