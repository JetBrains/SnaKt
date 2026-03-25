// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    @Unique var x = Any()
    @Unique var w = Any()
}

class B {
    @Unique var y = A()
}

fun consume(@Unique a: Any) {}

fun share(a: Any) {}

// Consuming subproperties

fun `consume shared subproperty`(z: B) {
    consume(<!UNIQUENESS_VIOLATION!>z.y.x<!>)
}

fun `consume borrowed subproperty`(@Borrowed z: B) {
    consume(<!UNIQUENESS_VIOLATION!>z.y.x<!>)
}

fun `consume unique subproperty`(@Unique z: B) {
    consume(z.y.x)
}

fun `consume unique-borrowed subproperty`(@Borrowed @Unique z: B) {
    consume(<!UNIQUENESS_VIOLATION!>z.y.x<!>)
}

fun `consume multiple unique subproperties`(@Unique z: B) {
    consume(z.y.x)
    consume(z.y.w)
}

// Consuming partially-inconsistent subproperties

fun `consume partially moved`(@Unique z: B) {
    consume(z.y)
    consume(<!UNIQUENESS_VIOLATION!>z<!>)
}

fun `consume partially shared`(@Unique z: B) {
    share(z.y)
    consume(<!UNIQUENESS_VIOLATION!>z<!>)
}

// Consuming subproperty after assignment

fun `consume unique parent after assigning subproperty to unique`(@Unique x: B, @Unique y: A) {
    x.y = y
    consume(x.y)
}
