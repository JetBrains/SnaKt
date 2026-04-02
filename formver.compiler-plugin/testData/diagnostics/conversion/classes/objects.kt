// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert

object SimpleSingleton {
    val x: Int = 42
}

fun <!VIPER_TEXT!>accessObjectProperty<!>(): Int {
    return SimpleSingleton.x
}

object WithMultipleFields {
    val a: Int = 1
    val b: Boolean = true
    var c: Int = 0
}

fun <!VIPER_TEXT!>accessMultipleObjectFields<!>() {
    val a = WithMultipleFields.a
    val b = WithMultipleFields.b
    val c = WithMultipleFields.c
}

fun <!VIPER_TEXT!>mutateObjectField<!>() {
    WithMultipleFields.c = 10
}

object WithMemberFunction {
    val value: Int = 5

    fun <!VIPER_TEXT!>getValue<!>(): Int {
        return value
    }
}

fun <!VIPER_TEXT!>callObjectMemberFunction<!>(): Int {
    return WithMemberFunction.getValue()
}

@NeverConvert
fun takeAny(x: Any) {}

fun <!VIPER_TEXT!>passObjectAsArgument<!>() {
    takeAny(SimpleSingleton)
}

interface HasValue {
    val value: Int
}

object ImplementingInterface : HasValue {
    override val value: Int = 99
}

fun <!VIPER_TEXT!>accessObjectViaInterface<!>(h: HasValue): Int {
    return h.value
}

fun <!VIPER_TEXT!>createAndPassObjectViaInterface<!>(): Int {
    return accessObjectViaInterface(ImplementingInterface)
}

open class Base(val n: Int)

object ExtendingClass : Base(7) {
    val extra: Int = 3
}

fun <!VIPER_TEXT!>accessObjectWithInheritance<!>() {
    val n = ExtendingClass.n
    val extra = ExtendingClass.extra
}
