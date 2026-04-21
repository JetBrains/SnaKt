// NEVER_VALIDATE


import org.jetbrains.kotlin.formver.plugin.Unique

class A() {
    @Unique
    var uniq: Int = 1
    var shared: Int = 2
}

class B() {
    @Unique
    var uniq: A = A()
    var shared: A = A()
}

class C() {
    @Unique
    var uniq: B = B()
    var shared: B = B()
}


fun <!VIPER_TEXT!>uniq_uniq<!>(@Unique c : C) {
    @Unique val l = c.uniq
}

fun <!VIPER_TEXT!>uniq_shared<!>(@Unique c: C) {
    val l = c.shared
}

fun <!VIPER_TEXT!>uniq_uniq_uniq<!>(@Unique c : C) {
    @Unique val l = c.uniq.uniq
}

fun <!VIPER_TEXT!>uniq_shared_uniq<!>(@Unique c: C) {
    val l = c.shared.uniq
}

fun <!VIPER_TEXT!>uniq_uniq_shared<!>(@Unique c : C) {
    val l = c.uniq.shared
}

fun <!VIPER_TEXT!>uniq_shared_shared<!>(@Unique c: C) {
    val l = c.shared.shared
}

fun <!VIPER_TEXT!>uniq_uniq_uniq_uniq<!>(@Unique c : C) {
    @Unique val l = c.uniq.uniq.uniq
}

fun <!VIPER_TEXT!>uniq_shared_uniq_uniq<!>(@Unique c: C) {
    val l = c.shared.uniq.uniq
}

fun <!VIPER_TEXT!>uniq_uniq_shared_uniq<!>(@Unique c : C) {
    val l = c.uniq.shared.uniq
}

fun <!VIPER_TEXT!>uniq_shared_shared_uniq<!>(@Unique c: C) {
    val l = c.shared.shared.uniq
}

fun <!VIPER_TEXT!>uniq_uniq_uniq_shared<!>(@Unique c : C) {
    val l = c.uniq.uniq.shared
}

fun <!VIPER_TEXT!>uniq_shared_uniq_shared<!>(@Unique c: C) {
    val l = c.shared.uniq.shared
}

fun <!VIPER_TEXT!>uniq_uniq_shared_shared<!>(@Unique c : C) {
    val l = c.uniq.shared.shared
}

fun <!VIPER_TEXT!>uniq_shared_shared_shared<!>(@Unique c: C) {
    val l = c.shared.shared.shared
}
