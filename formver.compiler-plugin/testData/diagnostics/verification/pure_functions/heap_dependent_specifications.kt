import org.jetbrains.kotlin.formver.plugin.*

class Node(val value: Int, val next: Node?)

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>isAlternating<!>(node: Node, expectsPositive: Boolean): Boolean {
    val isCurrentValid = if (expectsPositive) {
        node.value > 0
    } else {
        node.value < 0
    }
    val nextNode = node.next
    return isCurrentValid && if (nextNode != null) {
        isAlternating(nextNode, !expectsPositive)
    } else {
        true
    }
}

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>isStrictlyAscendingAndPositive<!>(node: Node): Boolean {
    preconditions {
        node.value >= 0
    }
    val nextNode = node.next
    return if (nextNode != null) {
        if (nextNode.value < 0) {
            false
        } else {
            nextNode.value > node.value && isStrictlyAscendingAndPositive(nextNode)
        }
    } else {
        true
    }
}


class TreeNode(val value: Int, val left: TreeNode?, val right: TreeNode?)

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>isLocallyValidBST<!>(node: TreeNode): Boolean {
    val leftNode = node.left
    val leftValid = if (leftNode != null) {
        leftNode.value < node.value && isLocallyValidBST(leftNode)
    } else {
        true
    }
    val rightNode = node.right
    val rightValid = if (rightNode != null) {
        rightNode.value > node.value && isLocallyValidBST(rightNode)
    } else {
        true
    }
    return leftValid && rightValid
}

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>testNode<!>(nodeLeft: Node, nodeRight: Node): Int {
    var node = nodeLeft
    if (nodeLeft.next != null) {
        node = nodeLeft.next
    } else if (nodeRight.next != null) {
        node = nodeRight.next
    }
    return node.value
}