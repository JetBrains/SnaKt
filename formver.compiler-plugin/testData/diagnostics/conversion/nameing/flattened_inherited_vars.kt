// NEVER_VALIDATE

open class Base_Cell(open var payload: Int) {
    open fun <!VIPER_TEXT!>read<!>() = payload
}

open class Base {
    open class Cell(open var payload: Boolean) {
        open fun <!VIPER_TEXT!>read<!>() = payload
    }
}

class Derived_Base_Cell(override var payload: Int) : Base_Cell(payload)

class Derived_Base {
    class Cell(override var payload: Boolean) : Base.Cell(payload)
}

fun <!VIPER_TEXT!>flattenedInheritedVars<!>() {
    val left = Derived_Base_Cell(1)
    val right = Derived_Base.Cell(false)
    left.payload = 2
    right.payload = true
    left.read()
    right.read()
}
