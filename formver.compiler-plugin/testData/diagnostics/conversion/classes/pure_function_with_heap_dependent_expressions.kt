import org.jetbrains.kotlin.formver.plugin.*

class Box(val data: Int)

class BoxOfBox(val box: Box)

class BoxOfBoxOfBox(val boxOfBox: BoxOfBox)

@Pure
fun <!VIPER_TEXT!>testData<!>(boxOfBox: BoxOfBox): Int {
    val value = boxOfBox.box.data
    return value
}

@Pure
fun <!VIPER_TEXT!>boxBoxBoxBox<!>(boxOfBoxOfBox: BoxOfBoxOfBox): Int {
    val value = boxOfBoxOfBox.boxOfBox.box.data
    return value
}

@Pure
fun <!VIPER_TEXT!>boxReturn<!>(boxOfBox: BoxOfBox): Int {
    return boxOfBox.box.data
}

data class Node<!VIPER_TEXT!>(
val <! VIPER_TEXT!>value<!>: Int,
val <! VIPER_TEXT!>next<!>: Node?)<!>

@Pure
fun <!VIPER_TEXT!>length<!>(node: Node): Int {
    return if (node.next == null) {
        1
    } else {
        1 + length(node.next)
    }
}

@Pure
fun getThisOrNextValue(node: Node): Int {
    return node.next?.value ?: node.value
}

fun <!VIPER_TEXT!>iAmAMethodAndNeedLength<!>(node: Node): Int {
    val length = if (node.next != null) length(node.next) else length(node)
    return length
}

fun <!VIPER_TEXT!>iAmAMethodAndNeedNextLength<!>(node: Node): Int {
    val length = if (node.next != null) length(node.next) else 0
    return length
}

fun <!VIPER_TEXT!>customLengthPreCondition<!>(node: Node): Int {
    preconditions {
        node.next != null
    }
    val length = if (node.next != null) length(node.next) else 0
    return length
}

@Pure
fun <!VIPER_TEXT!>variableReassign<!>(node: Node): Int {
    val nextNode = node.next
    val nextNode2 = nextNode
    var potentiallyNextValue = -1
    if (node.next != null) {
        potentiallyNextValue = nextNode2.value
    }
    return potentiallyNextValue
}

@Pure
fun id(node: Node?): Node? {
    return node
}

@Pure
fun getNextValueFromIdentity(node: Node): Int {
    val nextNode = id(node.next)
    var nextValue = 0
    if (nextNode != null) {
        nextValue = nextNode.value
    }
    return nextValue
}
