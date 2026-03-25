// NEVER_VALIDATE
package top.second.third

class Foo() {
    val bar: Int = 12
    val baz: Int = 13

    class Bar(val foo: Int) {

        var bar: Int
            get() {
                return 5
            }

        var baz: Int4
            get() {
                return 5
            }

    }
}

fun test() {
    val bar = Foo.Bar(5)
    val foo = Foo()
}


// Gets Renamed to:
//root -> root
//root.top -> top
//root.top.second -> second
//root.top.second.third -> third
//root.top.second.third.Foo -> Foo
//root.top.second.third.Foo.Bar -> Bar
//root.top.second.third.Foo.Bar.foo -> foo
//root.top.second.third.Foo.Bar.bar -> field_bar
//root.top.second.third.Foo.Bar.bar.get -> field_bar_get
//root.top.second.third.Foo.Bar.baz -> field_baz
//root.top.second.third.Foo.Bar.baz.get -> get
//root.top.second.third.Foo.Bar.baz.set -> set
//type_Int -> Int
//type_Int! -> Int!
//funType(type_Int!) -> root.top.second.third.Foo.Bar -> (Int!)_to_Bar
//root.top.second.third.Foo.Bar.con -> Bar_con
//root.top.second.third.Foo.bar -> bar
//root.top.second.third.Foo.baz -> baz
//funType() -> root.top.second.third.Foo -> ()_to_Foo
//root.top.second.third.Foo.con -> con


package top.second.third
class Foo() /*constructor: con */ {
    val bar: Int = 12
    val baz: Int = 13

    class Bar(val foo: Int) /*constructor: Bar_con */ {

        var field_bar: Int
            get() /* field_bar_get */ {
                return 5
            }

        var field_baz: Int
            get() /* set */ {
                return 5
            }

    }
}

fun test() {
    val bar = Foo.Bar(5)
    val foo = Foo()
}