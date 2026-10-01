// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
fun <!VIPER_TEXT!>lengthAndEquality<!>(left: String, right: String) {
    verify(
        left.length >= 0,
        (left == right) == (right == left),
        (left + right).length == left.length + right.length,
        (left + "").length == left.length,
        ("" + right).length == right.length,
    )
}

@AlwaysVerify
fun <!VIPER_TEXT!>shortConcreteStrings<!>() {
    verify(
        "".length == 0,
        "a".length == 1,
        "b".length == 1,
        "aa".length == 2,
        "ab".length == 2,
        "ba".length == 2,
        "bb".length == 2,
        "" != "a",
        "a" != "b",
        "a" != "aa",
        "ab" == "a" + "b",
        "ba" == "b" + "a",
        "aa"[0] == 'a',
        "aa"[1] == 'a',
        "ab"[0] == 'a',
        "ab"[1] == 'b',
        "ba"[0] == 'b',
        "ba"[1] == 'a',
        "bb"[0] == 'b',
        "bb"[1] == 'b',
    )
}

@AlwaysVerify
fun String.<!VIPER_TEXT!>firstTwo<!>(): String {
    preconditions { length >= 2 }
    postconditions<String> { result ->
        result.length == 2
        result[0] == this@firstTwo[0]
        result[1] == this@firstTwo[1]
    }

    return "" + this[0] + this[1]
}

@AlwaysVerify
fun <!VIPER_TEXT!>guardedBoundaryIndex<!>(value: String) {
    if (value.length > 0) {
        val first = value[0]
        val last = value[value.length - 1]
        verify(first == value[0], last == value[value.length - 1])
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>emptyStringIndex<!>() {
    val impossible = <!VIPER_VERIFICATION_ERROR!>""[0]<!>
}
