// WITH_STDLIB

fun <!VIPER_TEXT!>`p$shared`<!>() {}

fun <!VIPER_TEXT!>predicateMethodCollectionBaseConsistencyError<!>(xs: List<Int>) {
    `p$shared`()
    xs.isEmpty()
}
