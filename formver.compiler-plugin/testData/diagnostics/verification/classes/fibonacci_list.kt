import org.jetbrains.kotlin.formver.plugin.*

class Node(val value: Int, val next: Node?)

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>fib<!>(n: Int): Int {
    preconditions {
        n >= 0
    }
    if (n == 0) return 0
    if (n == 1) return 1
    return fib(n - 1) + fib(n - 2)
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>isFibList<!>(head: Node, length: Int): Boolean {
    preconditions {
        length >= 1
    }
    if (head.value != fib(length)) {
        return false
    }
    if (length == 1) {
        return head.next == null
    }
    if (head.next == null) {
        return false
    }
    val isFib = isFibList(head.next, length - 1)
    return isFib
}

@AlwaysVerify
fun <!VIPER_TEXT!>buildFibList<!>(length: Int): Node {
    preconditions {
        length >= 1
    }
    postconditions<Node> { res ->
        isFibList(res, length)
    }

    if (length == 1) {
        return Node(fib(1), null)
    }

    val tail = buildFibList(length - 1)
    return Node(fib(length), tail)
}

// --- LIST METHODS ---

@AlwaysVerify
fun <!VIPER_TEXT!>prependFib<!>(head: Node, length: Int): Node {
    preconditions {
        length >= 1
        isFibList(head, length)
    }
    postconditions<Node> { res ->
        isFibList(res, length + 1)
    }

    return Node(fib(length + 1), head)
}

@AlwaysVerify
fun <!VIPER_TEXT!>dropFirst<!>(head: Node, length: Int): Node {
    preconditions {
        length >= 2
        isFibList(head, length)
    }
    postconditions<Node> { res ->
        isFibList(res, length - 1)
    }

    val nextNode = head.next
    if (nextNode == null) {
        return head // Unreachable per FormVer, satisfies compiler
    }

    return nextNode
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>getFirst<!>(head: Node, length: Int): Int {
    preconditions {
        length >= 1
        isFibList(head, length)
    }
    postconditions<Int> { res ->
        res == fib(length)
    }
    return head.value
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>getLast<!>(head: Node, length: Int): Int {
    preconditions {
        length >= 1
        isFibList(head, length)
    }
    postconditions<Int> { res ->
        res == fib(1)
    }

    if (length == 1) {
        return head.value
    }

    val nextNode = head.next
    if (nextNode == null) {
        return 0 // Unreachable per FormVer, satisfies compiler
    }

    return getLast(nextNode, length - 1)
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>getNth<!>(head: Node, length: Int, index: Int): Int {
    preconditions {
        length >= 1
        index >= 0
        index < length
        isFibList(head, length)
    }
    postconditions<Int> { res ->
        res == fib(length - index)
    }

    if (index == 0) {
        return head.value
    }

    val nextNode = head.next
    if (nextNode == null) {
        return 0 // Unreachable per FormVer, satisfies compiler
    }

    return getNth(nextNode, length - 1, index - 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>sumFibList<!>(head: Node, length: Int): Int {
    preconditions {
        length >= 1
        isFibList(head, length)
    }
    postconditions<Int> { res ->
        res == fib(length + 2) - 1
    }

    if (length == 1) {
        val fib0 = fib(0)
        val fib1 = fib(2)
        val fib3 = fib(3)
        return head.value
    }

    val nextNode = head.next
    if (nextNode == null) {
        return fib(length + 2) - 1 // Unreachable per FormVer, satisfies compiler
    }

    val tailSum = sumFibList(nextNode, length - 1)
    return head.value + tailSum
}

@AlwaysVerify
fun <!VIPER_TEXT!>copyFibList<!>(head: Node, length: Int): Node {
    preconditions {
        length >= 1
        isFibList(head, length)
    }
    postconditions<Node> { res ->
        isFibList(res, length)
    }

    if (length == 1) {
        return Node(head.value, null)
    }

    val nextNode = head.next
    if (nextNode == null) {
        return Node(head.value, null) // Unreachable per FormVer, satisfies compiler
    }

    val copiedTail = copyFibList(nextNode, length - 1)
    return Node(head.value, copiedTail)
}
