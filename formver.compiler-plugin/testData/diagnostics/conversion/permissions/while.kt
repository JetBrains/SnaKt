import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

class A(
    @Unique var um: B,  // unique-mutable
    @Unique val ui: B,  // unique-immutable
    var sm: B,          // shared-mutable
    val si: B,          // shared-immutable
)


class B(
    @Unique var um: C,  // unique-mutable
    @Unique val ui: C,  // unique-immutable
    var sm: C,          // shared-mutable
    val si: C,          // shared-immutable
)


class C(
    @Unique var um: Any,  // unique-mutable
    @Unique val ui: Any,  // unique-immutable
    var sm: Any,          // shared-mutable
    val si: Any,          // shared-immutable
)

class D(
    @Unique var um: A?
)

class Simple(
    @Unique var something: Any
)

fun <!VIPER_TEXT!>simpleWrite<!>(@Unique a: Simple) {

    while (nonDet()) {
        // unique(a)
        a.something = Any()
    }

    consume(a)
}

fun <!VIPER_TEXT!>nonDet<!>() : Boolean {
    return true
}

fun <!VIPER_TEXT!>consume<!>(@Unique a: Simple) {}
fun <!VIPER_TEXT!>consume<!>(@Unique a: A) {}
fun <!VIPER_TEXT!>borrow<!>(@Unique @Borrowed b: B) {}
fun <!VIPER_TEXT!>borrow<!>(@Unique @Borrowed a: A) {}


fun <!VIPER_TEXT!>simple<!>(@Unique a: A) {

    while (nonDet()) {

    }

    consume(a)
}


fun <!VIPER_TEXT!>partiallyMovedOverLoop<!>(@Unique a: A) {
    @Unique val x = a.um

    while (nonDet()) {
        // shared(a.sm)
        // shared(a.si)
        // unique(a.um)
        if (a.sm == a.si) {

        }
    }
}

fun <!VIPER_TEXT!>borrowInLoop<!>(@Unique a: A) {

    while (nonDet()) {
        // unique(a)
        if (a.sm == a.si) {
            borrow(a.um)
        }
    }
}

fun <!VIPER_TEXT!>unfoldForCondition<!>(@Unique a: A) {

    while (a.um == a.ui) {
    }
}

class Node(
    @Unique var next: Node?,
    @Unique var data: A,
)

fun <!VIPER_TEXT!>optional<!>(@Unique n: Node) {

    while (nonDet() && n.next != null) {
        borrow(n.data)
    }
}
