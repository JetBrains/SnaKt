// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.*

class Box(val inner: Box?)

@Pure
fun <!VIPER_TEXT!>elvisThenField<!>(x: Box, y: Box): Box? {
    return (x.inner ?: y).inner
}

@Pure
fun <!VIPER_TEXT!>elvisThenSafeField<!>(x: Box?, y: Box): Box? {
    return (x ?: y).inner
}

@Pure
fun <!VIPER_TEXT!>elvisThenSafeChain<!>(x: Box?, y: Box?): Box? {
    return (x ?: y)?.inner?.inner
}
