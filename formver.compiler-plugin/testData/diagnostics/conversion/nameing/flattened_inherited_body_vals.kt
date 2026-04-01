// NEVER_VALIDATE

open class Base_BodyVal(seed: Int) {
    open val payload = seed
}

open class Base {
    open class BodyVal(seed: Boolean) {
        open val payload = seed
    }
}

class Derived_Base_BodyVal(seed: Int) : Base_BodyVal(seed)

class Derived_Base {
    class BodyVal(seed: Boolean) : Base.BodyVal(seed)
}

fun <!VIPER_TEXT!>flattenedInheritedBodyVals<!>() {
    val left = Derived_Base_BodyVal(1)
    val right = Derived_Base.BodyVal(true)
    left.payload
    right.payload
}
