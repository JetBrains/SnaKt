// FULL_JDK

fun <!VIPER_TEXT!>subjectlessFirstMatchWins<!>(x: Int): Int = when {
    x >= 0 -> 1
    x > 0 -> 2
    else -> 3
}

fun <!VIPER_TEXT!>subjectlessUnreachableElse<!>(x: Int): Int = when {
    x < 0 -> -1
    x >= 0 -> 1
    else -> 99
}

fun <!VIPER_TEXT!>subjectFirstMatchWins<!>(x: Int): Int = when (x) {
    0, 1 -> 10
    1 -> 20
    else -> 30
}

fun <!VIPER_TEXT!>exhaustiveBooleanSubject<!>(value: Boolean): Int = when (value) {
    false -> 0
    true -> 1
}

fun <!VIPER_TEXT!>guardedSubjectBranch<!>(value: Any): Int = when (value) {
    is Int if value > 0 -> 1
    is Int -> -1
    else -> 0
}

fun <!VIPER_TEXT!>nestedReturnFromSubjectlessWhen<!>(x: Int, stop: Boolean): Int {
    return when (x) {
        0 -> when {
            stop -> return 10
            else -> 11
        }
        else -> when {
            x < 0 -> return -1
            else -> 12
        }
    }
}

fun <!VIPER_TEXT!>subjectlessNearBoundary<!>(x: Int): Int {
    when {
        x == 0 -> return 0
        x < 0 -> return -1
    }
    return 1
}
