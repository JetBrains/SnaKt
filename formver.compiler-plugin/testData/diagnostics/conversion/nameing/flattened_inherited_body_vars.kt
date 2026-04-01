// NEVER_VALIDATE

open class Base_BodyVar(seed: Int) {
    open var payload = seed
}

open class Base {
    open class BodyVar(seed: Boolean) {
        open var payload = seed
    }
}

class Derived_Base_BodyVar(seed: Int) : Base_BodyVar(seed)

class Derived_Base {
    class BodyVar(seed: Boolean) : Base.BodyVar(seed)
}

fun <!VIPER_TEXT!>flattenedInheritedBodyVars<!>() {
    val left = Derived_Base_BodyVar(1)
    val right = Derived_Base.BodyVar(true)
    left.payload = 2
    right.payload = false
}
