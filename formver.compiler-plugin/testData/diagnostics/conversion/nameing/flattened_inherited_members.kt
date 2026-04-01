// NEVER_VALIDATE

open class Base_Node(open val payload: Int) {
    open fun <!VIPER_TEXT!>read<!>() = payload
}

open class Base {
    open class Node(open val payload: Boolean) {
        open fun <!VIPER_TEXT!>read<!>() = payload
    }
}

class Derived_Base_Node(override val payload: Int) : Base_Node(payload)

class Derived_Base {
    class Node(override val payload: Boolean) : Base.Node(payload)
}

fun <!VIPER_TEXT!>flattenedInheritedMembers<!>() {
    val left = Derived_Base_Node(1)
    val right = Derived_Base.Node(false)
    left.payload
    right.payload
    left.read()
    right.read()
}
