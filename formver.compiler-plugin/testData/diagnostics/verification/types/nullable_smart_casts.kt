fun <!VIPER_TEXT!>stableAliasRetainsNonNullFact<!>(input: Int?): Int {
    val stable = input
    if (stable != null) {
        return stable
    }
    return 0
}

fun <!VIPER_TEXT!>repeatedNullChecks<!>(input: Int?): Int {
    if (input == null) return 0
    if (<!SENSELESS_COMPARISON!>input == null<!>) return 1
    return input
}

fun <!VIPER_TEXT!>branchSelectedValueIsNonNull<!>(chooseFirst: Boolean, first: Int?, second: Int?): Int {
    if (first == null || second == null) return 0
    val selected = if (chooseFirst) first else second
    return selected
}

fun <!VIPER_TEXT!>elvisProducesNonNullValue<!>(input: Int?): Int = input ?: 17

class NullableSmartCastBox(val value: Int)

fun <!VIPER_TEXT!>safeCallFollowsNullBranch<!>(box: NullableSmartCastBox?): Int? {
    val result = box?.value
    if (box == null) {
        return result
    }
    return result
}

fun <!VIPER_TEXT!>stableAliasSurvivesMutableLocalMutation<!>(input: Int?): Int {
    var current = input
    if (current != null) {
        val stableBeforeMutation: Int = current
        current = null
        return stableBeforeMutation
    }
    return 0
}

fun <!VIPER_TEXT!>elvisAfterMutationUsesCurrentValue<!>(input: Int?, replacement: Int?): Int {
    var current = input
    if (current != null) {
        current = replacement
    }
    return current ?: 19
}

fun <!VIPER_TEXT!>mutableLocalCanBeCheckedAgain<!>(input: Int?, replacement: Int?): Int {
    var current = input
    if (current != null) {
        current = replacement
    } else {
        current = 1
    }
    if (current != null) return current
    return 0
}
