// NEVER_VALIDATE

open class Base_Action {
    open fun <!VIPER_TEXT!>act<!>(): Int = 1
}

open class Base {
    open class Action {
        open fun <!VIPER_TEXT!>act<!>(): Boolean = true
    }
}

class Derived_Base_Action : Base_Action()

class Derived_Base {
    class Action : Base.Action()
}

fun <!VIPER_TEXT!>flattenedInheritedFunctions<!>() {
    val left = Derived_Base_Action()
    val right = Derived_Base.Action()
    left.act()
    right.act()
}
