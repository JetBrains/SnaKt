import org.jetbrains.kotlin.formver.plugin.*

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
    @Unique val l = c.unique
}

fun <!VIPER_TEXT!>uniq_shared<!>(@Unique c: C) {
    val l = c.shared
}

fun <!VIPER_TEXT!>uniq_uniq_uniq<!>(@Unique c : C) {
    @Unique val l = c.unique.unique
}

fun <!VIPER_TEXT!>uniq_shared_uniq<!>(@Unique c: C) {
    val l = c.shared.unique
}

fun <!VIPER_TEXT!>uniq_uniq_shared<!>(@Unique c : C) {
    val l = c.unique.shared
}

fun <!VIPER_TEXT!>uniq_shared_shared<!>(@Unique c: C) {
    val l = c.shared.shared
}

fun <!VIPER_TEXT!>uniq_uniq_uniq_uniq<!>(@Unique c : C) {
    @Unique val l = c.unique.unique.unique
}

fun <!VIPER_TEXT!>uniq_shared_uniq_uniq<!>(@Unique c: C) {
    val l = c.shared.unique.unique
}

fun <!VIPER_TEXT!>uniq_uniq_shared_uniq<!>(@Unique c : C) {
    val l = c.unique.shared.unique
}

fun <!VIPER_TEXT!>uniq_shared_shared_uniq<!>(@Unique c: C) {
    val l = c.shared.shared.unique
}

fun <!VIPER_TEXT!>uniq_uniq_uniq_shared<!>(@Unique c : C) {
    val l = c.unique.unique.shared
}

fun <!VIPER_TEXT!>uniq_shared_uniq_shared<!>(@Unique c: C) {
    val l = c.shared.unique.shared
}

fun <!VIPER_TEXT!>uniq_uniq_shared_shared<!>(@Unique c : C) {
    val l = c.unique.shared.shared
}

fun <!VIPER_TEXT!>uniq_shared_shared_shared<!>(@Unique c: C) {
    val l = c.shared.shared.shared
}
