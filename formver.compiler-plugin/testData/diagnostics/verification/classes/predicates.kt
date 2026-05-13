// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

open class Baz()

class PrimitiveFields(val a: Int, var b: Int)

class ReferenceField(val pf: PrimitiveFields) : Baz()

class Recursive(val next: Recursive?)

fun <!VIPER_TEXT!>useClasses<!>(rf: ReferenceField, rec: Recursive) { }

open class A() {
    val x: Int = 1
    var y: Int = 2
}
open class B() : A()
class C() : B()

fun <!VIPER_TEXT!>threeLayersHierarchy<!>(c: C) { }

fun <!VIPER_TEXT!>listHierarchy<!>(xs: MutableList<Int>) { }

class T()

open class S()

class Foo(val w: Int, var x: Int, @property:Unique val y: T, @property:Unique var z: T) : S()

fun <!VIPER_TEXT!>unique_foo_arg<!>(@Unique foo: Foo) {}

fun <!VIPER_TEXT!>nullable_unique_arg<!>(@Unique t: T?) {}

fun <!VIPER_TEXT!>borrowed_unique_arg<!>(@Unique @Borrowed t: T) {}

fun @receiver:Unique T.<!VIPER_TEXT!>unique_receiver<!>() {}

fun @receiver:Unique @receiver:Borrowed T.<!VIPER_TEXT!>borrowed_unique_receiver<!>() {}

@Unique
fun <!VIPER_TEXT!>unique_result<!>() : T { return T() }

@Unique
fun <!VIPER_TEXT!>unique_nullable_result<!>() : T? { return null }
