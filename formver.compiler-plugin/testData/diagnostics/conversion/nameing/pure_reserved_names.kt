// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>pureReservedNames<!>(`result`: Int, `p$a`: Int): Int {
    val `result$0` = `result`
    val `anon` = `p$a`
    return `result$0` + `anon`
}

fun <!VIPER_TEXT!>callPureReservedNames<!>() {
    pureReservedNames(1, 2)
}
