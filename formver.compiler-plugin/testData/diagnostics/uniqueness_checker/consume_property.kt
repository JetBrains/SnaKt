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

// Consuming subproperties

fun `consume shared subproperty`(z: B) {
    consume(<!UNIQUENESS_MISMATCH!>z.y.x<!>)
}

fun `consume borrowed subproperty`(z: @Borrowed B) {
    consume(<!UNIQUENESS_MISMATCH!>z.y.x<!>)
}

fun `consume unique subproperty`(z: @Unique B) {
    consume(z.y.x)
}

fun `consume unique-borrowed subproperty`(z: @Borrowed @Unique B) {
    consume(z.y.x)
}

fun `consume multiple unique subproperties`(z: @Unique B) {
    consume(z.y.x)
    consume(z.y.w)
}

// Consuming partially-inconsistent subproperties

fun `consume partially moved`(z: @Unique B) {
    consume(z.y)
    // TODO: Check for partially moved references at function boundaries
    consume(<!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>z<!>)
}

fun `consume partially shared`(z: @Unique B) {
    share(z.y)
    // TODO: Check for partially moved references at function boundaries
    consume(<!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>z<!>)
}

// Consuming subproperty after assignment

fun `consume unique parent after assigning subproperty to unique`(x: @Unique B, y: @Unique A) {
    x.y = y
    consume(x.y)
}

// Consuming subproperty after smart-cast

class Node(
    val next: @Unique Node?
)

fun `consume unique parent after cast`(node: @Unique Any) {
    val local: @Unique Node? = (node as Node).next
    // TODO: Check for partially moved references at function boundaries
    consume(<!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>node<!>)
}

fun `consume unique parent after cast to not-null`(node: @Unique Node) {
    val local: @Unique Node = node.next as Node
    // TODO: Check for partially moved references at function boundaries
    consume(<!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>node<!>)
}

fun `consume unique parent after smart-cast`(node: @Unique Node?) {
    if (node != null) {
        val local: @Unique Node? = node.next
        // TODO: Check for partially moved references at function boundaries
        consume(<!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>node<!>)
    }
}
