// NEVER_VALIDATE

open class Base_CustomSetter {
    open var payload: Int = 0
        set(v) {
            field = v
        }
}

open class Base {
    open class CustomSetter {
        open var payload: Boolean = false
            set(v) {
                field = v
            }
    }
}

class Derived_Base_CustomSetter : Base_CustomSetter()

class Derived_Base {
    class CustomSetter : Base.CustomSetter()
}

fun <!VIPER_TEXT!>flattenedInheritedCustomSetters<!>() {
    val left = Derived_Base_CustomSetter()
    val right = Derived_Base.CustomSetter()
    left.payload = 1
    right.payload = true
}
