// NEVER_VALIDATE
// WITH_STDLIB
// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>`p$pkg$kotlin_collections$c$List$shared`<!>(x: Int): Int = x + 1

fun <!VIPER_TEXT!>predicateCollectionHelperNameCollision<!>(xs: List<Int>) {
    `p$pkg$kotlin_collections$c$List$shared`(0)
    xs.isEmpty()
}
