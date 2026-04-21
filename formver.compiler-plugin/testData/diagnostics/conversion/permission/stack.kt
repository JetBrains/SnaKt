import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

class Node(
    @Unique var next: Node?,
    var data: Int
)

class Stack(
    @Unique var head: Node?
)

fun <!VIPER_TEXT!>pop<!>(@Unique stack: Stack) : Node? {
    @Unique var head = stack.head
    if (head != null) {
        @Unique var nextNode = head.next
        head.next = null
        stack.head = nextNode
        return head
    } else {
        return null
    }
}

fun <!VIPER_TEXT!>insert<!>(@Unique stack: Stack, @Unique node: Node) {
    @Unique val head = stack.head
    if (head != null) {
        node.next = head
        stack.head = node
    } else {
        stack.head = node
    }
}
