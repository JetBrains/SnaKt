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

fun consume(a: @Unique Any) {}

fun share(a: Any) {}

// Sharing subproperties

fun `share shared subproperty`(z: B) {
    share(z.y.x)
}

fun `share borrowed subproperty`(z: @Borrowed B) {
    share(z.y.x)
}

fun `share unique subproperty`(z: @Unique B) {
    share(z.y.x)
}

fun `share unique-borrowed subproperty`(z: @Unique @Borrowed B) {
    share(z.y.x)
}

fun `share multiple unique subproperties`(z: @Unique B) {
    share(z.y.x)
    share(z.y.w)
}

// Sharing partially-inconsistent properties

fun `share partially moved`(z: @Unique B) {
    consume(z.y)
    share(<!UNIQUENESS_MISMATCH!>z<!>)
}

fun `share partially shared`(z: @Unique B) {
    share(z.y)
    share(z)
}

// Sharing subproperties after assignment

fun `share subproperty after assigning it to shared`(x: @Unique B, v: A) {
    x.y = v
    share(x.y)
}

fun `share subproperty after assigning it to unique`(x: @Unique B, v: @Unique A) {
    x.y = v
    share(x.y)
}
