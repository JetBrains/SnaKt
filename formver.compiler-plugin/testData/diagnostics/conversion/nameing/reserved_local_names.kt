// NEVER_VALIDATE

fun <!VIPER_TEXT!>reservedLocalNames<!>() {
    val `ret` = 1
    val `anon` = true
    val `ret$0` = `ret`
    val `anon$0` = `anon`
    val `lbl$ret$0` = `ret$0`
    val x = `lbl$ret$0`
    if (`anon$0`) {
        val `ret` = x + 1
        val y = `ret`
    }
}
