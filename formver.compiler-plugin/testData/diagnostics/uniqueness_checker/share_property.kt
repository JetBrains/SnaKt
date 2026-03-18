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

// Sharing subproperties

fun `share shared subproperty`(z: B) {
    share(z.y.x)
}

fun `share borrowed subproperty`(@Borrowed z: B) {
    share(<!UNIQUENESS_VIOLATION!>z.y.x<!>)
}

fun `share unique subproperty`(@Unique z: B) {
    share(z.y.x)
}

fun `share unique-borrowed subproperty`(@Unique @Borrowed z: B) {
    share(<!UNIQUENESS_VIOLATION!>z.y.x<!>)
}

fun `share multiple unique subproperties`(@Unique z: B) {
    share(z.y.x)
    share(z.y.w)
}

// Sharing partially-inconsistent properties

fun `share partially moved`(@Unique z: B) {
    consume(z.y)
    share(<!UNIQUENESS_VIOLATION!>z<!>)
}

fun `share partially shared`(@Unique z: B) {
    share(z.y)
    share(<!UNIQUENESS_VIOLATION!>z<!>)
}

// Sharing subproperties after assignment

fun `share subproperty after assigning it to shared`(@Unique x: B, v: A) {
    x.y = <!UNIQUENESS_VIOLATION!>v<!>
    share(x.y)
}

fun `share subproperty after assigning it to unique`(@Unique x: B, @Unique v: A) {
    x.y = v
    share(x.y)
}
