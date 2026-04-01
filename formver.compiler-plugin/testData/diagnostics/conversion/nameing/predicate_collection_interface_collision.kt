// NEVER_VALIDATE
// WITH_STDLIB
// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>`Collection$p$shared`<!>(x: Int): Int = x

fun <!VIPER_TEXT!>predicateCollectionInterfaceCollision<!>(xs: List<Int>) {
    `Collection$p$shared`(0)
    xs.isEmpty()
}
