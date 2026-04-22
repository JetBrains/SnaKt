// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>safeDivide<!>(x: Int, y: Int): Int {
    var res = 0
    if (y != 0) {
        res = x / y
    }
    return res
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>getStringLength<!>(obj: Any): Int {
    var len = -1
    if (obj is String) {
        len = obj.length
    }
    return len
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>safeNestedDivide<!>(x: Int, y: Int, z: Int): Int {
    var res = 0
    if (y != 0) {
        if (z != 0) {
            res = (x / y) / z
        }
    }
    return res
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>safeInverseDifference<!>(x: Int, y: Int): Int {
    var res = 0
    if (x != y) {
        res = 100 / (x - y)
    }
    return res
}
