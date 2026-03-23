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


fun <!VIPER_TEXT!>consume<!>(@Unique c: C) {}
fun <!VIPER_TEXT!>consume<!>(@Unique b: B) {}
fun <!VIPER_TEXT!>shared<!>(c: C) {}
fun <!VIPER_TEXT!>borrow<!>(@Borrowed b: B) {}
fun <!VIPER_TEXT!>consumeTwo<!>(@Unique b1: B, @Unique b2: B) {}

fun <!VIPER_TEXT!>testUniqueFieldFoldUnfold<!>(@Unique a: A) {

    consume(a.um)
}

fun <!VIPER_TEXT!>testBorrowedField<!>(@Unique a: A) {
    borrow(a.um)
    borrow(a.um)
}

fun <!VIPER_TEXT!>testNestedUnique<!>(@Unique a: A) {
    @Unique val b = a.um
    consume(b.um)
    a.um = b
}


fun <!VIPER_TEXT!>testMultipleCalls<!>(@Unique a: A) {
    borrow(a.ui)
    borrow(a.ui)
}


fun <!VIPER_TEXT!>testNestedBorrow<!>(@Unique a: A) {
    borrow(a.um)
    @Unique @Borrowed val c = a.um.um
}

fun <!VIPER_TEXT!>testMixedAccess<!>(@Unique a: A) {
    borrow(a.um)
    consume(a.um)
}

fun <!VIPER_TEXT!>testTwo<!>(@Unique a: A) {
    consumeTwo(a.um, a.ui)
}

fun <!VIPER_TEXT!>testSharedField<!>(@Unique a: A) {
    @Unique val b = a.um
    shared(b.sm)
    a.um = b
}


