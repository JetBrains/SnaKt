// NEVER_VALIDATE

class InnerValue_Box {
    inner class Node(val value: Int)
}

class InnerValue {
    inner class Box {
        inner class Node(val value: Boolean)
    }
}

fun <!VIPER_TEXT!>innerClassPropertyCollision<!>() {
    val left = InnerValue_Box().Node(1)
    val right = InnerValue().Box().Node(true)
    left.value
    right.value
}
