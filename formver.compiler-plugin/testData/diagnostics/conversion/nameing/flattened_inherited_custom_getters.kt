// NEVER_VALIDATE

open class Base_CustomGetter {
    open val payload: Int
        get() = 1
}

open class Base {
    open class CustomGetter {
        open val payload: Boolean
            get() = true
    }
}

class Derived_Base_CustomGetter : Base_CustomGetter()

class Derived_Base {
    class CustomGetter : Base.CustomGetter()
}

fun <!VIPER_TEXT!>flattenedInheritedCustomGetters<!>() {
    val left = Derived_Base_CustomGetter()
    val right = Derived_Base.CustomGetter()
    left.payload
    right.payload
}
