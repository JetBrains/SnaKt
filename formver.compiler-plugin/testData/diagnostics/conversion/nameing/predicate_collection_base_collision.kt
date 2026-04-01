// NEVER_VALIDATE
// WITH_STDLIB
// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>`p$shared`<!>(x: Int): Int = x

fun <!VIPER_TEXT!>predicateCollectionBaseCollision<!>(xs: List<Int>) {
    `p$shared`(0)
    xs.isEmpty()
}
