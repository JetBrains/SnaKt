// WITH_STDLIB
// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>isEmpty<!>(x: Int): Int = x

class Collection_Box(val seed: Int) {
    val isEmpty: Int
        get() = seed
}

class Collection {
    class Box(val seed: Boolean) {
        val isEmpty: Boolean
            get() = seed
    }
}

fun <!VIPER_TEXT!>collectionNameShadowing<!>(l: List<Int>) {
    val left = Collection_Box(1)
    val right = Collection.Box(true)
    isEmpty(0)
    left.isEmpty
    right.isEmpty
    l.isEmpty()
}
