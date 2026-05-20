import org.jetbrains.kotlin.formver.plugin.Unique

class A() {
    @Unique
    var unique: Int = 1
    var shared: Int = 2
}

class B() {
    @Unique
    var unique: A = A()
    var shared: A = A()
}

class C() {
    @Unique
    var unique: B = B()
    var shared: B = B()
}


fun <!VIPER_TEXT!>uniq_uniq<!>(@Unique c : C) {
    c.unique = B()
}

fun <!VIPER_TEXT!>uniq_shared<!>(@Unique c: C) {
    c.shared = B()
}

fun <!VIPER_TEXT!>uniq_uniq_uniq<!>(@Unique c : C) {
    c.unique.unique = A()
}

fun <!VIPER_TEXT!>uniq_shared_uniq<!>(@Unique c: C) {
    c.shared.unique = A()
}

fun <!VIPER_TEXT!>uniq_uniq_shared<!>(@Unique c : C) {
    c.unique.shared = A()
}

fun <!VIPER_TEXT!>uniq_shared_shared<!>(@Unique c: C) {
    c.shared.shared = A()
}

fun <!VIPER_TEXT!>uniq_uniq_uniq_uniq<!>(@Unique c : C) {
    c.unique.unique.unique = 1
}

fun <!VIPER_TEXT!>uniq_shared_uniq_uniq<!>(@Unique c: C) {
    c.shared.unique.unique = 2
}

fun <!VIPER_TEXT!>uniq_uniq_shared_uniq<!>(@Unique c : C) {
    c.unique.shared.unique = 3
}

fun <!VIPER_TEXT!>uniq_shared_shared_uniq<!>(@Unique c: C) {
    c.shared.shared.unique = 4
}

fun <!VIPER_TEXT!>uniq_uniq_uniq_shared<!>(@Unique c : C) {
    c.unique.unique.shared = 5
}

fun <!VIPER_TEXT!>uniq_shared_uniq_shared<!>(@Unique c: C) {
    c.shared.unique.shared = 6
}

fun <!VIPER_TEXT!>uniq_uniq_shared_shared<!>(@Unique c : C) {
    c.unique.shared.shared = 7
}

fun <!VIPER_TEXT!>uniq_shared_shared_shared<!>(@Unique c: C) {
    c.shared.shared.shared = 8
}
