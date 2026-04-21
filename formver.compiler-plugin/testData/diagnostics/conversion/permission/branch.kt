// NEVER_VALIDATE
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

class Node(@Unique var left: Node?, @Unique var right: Node?)

fun <!VIPER_TEXT!>conditionVariable<!>(@Unique root: Node) {
    val condition = root.left == root.right
    if (condition) {
        root.left = null
    }
}

fun <!VIPER_TEXT!>bothBranches<!>(@Unique root: Node) {
    if (root.left == root.right) {
        root.left = null
    } else {
        root.right = null
    }
}


fun <!VIPER_TEXT!>oneBranches<!>(@Unique root: Node) {
    if (root.left == root.right) {
        root.left = null
    }
}


fun <!VIPER_TEXT!>consume<!>(@Unique node: Node?) {}

fun <!VIPER_TEXT!>movedBeforeBranch<!>(@Unique root: Node) {

    consume(root.left)

    if (root.right == null) {
        root.right = null
        root.left = null
    }

}


fun <!VIPER_TEXT!>consume<!>(@Unique a: Node?, @Unique b: Node?) {}
fun <!VIPER_TEXT!>movedBeforeBranch<!>(@Unique root: Node, @Unique root2: Node) {

    consume(root.left, root2.right)

    if (root.right == null) {
        root2.right = null
        root2.left = null
    } else {
        root2.right = null
        root2.left = null
    }

    consume(root2.right, root2.left)

}