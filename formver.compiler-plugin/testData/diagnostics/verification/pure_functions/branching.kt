// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>noAssignmentInBlocks<!>(a: Boolean): Int {
    var result = 0
    if (a) {
    } else {
    }
    result = result + 1
    return result
}

@Pure
fun <!VIPER_TEXT!>simpleBranching<!>(a: Boolean): Int {
    var result = 0
    if (a) {
        result = result + 1
    } else {
        result = result + 2
    }
    return result
}

@Pure
fun <!VIPER_TEXT!>nestedBranching<!>(a: Boolean, b: Boolean): Int {
    var result = 0
    if (a) {
        result = 1 + result
        if (b) {
            result = 2 + result
        } else {
            result = 3 + result
        }
    } else {
        if (b) {
            result = 4 + result
        } else {
            result = 5 + result
        }
        result = 6 + result
    }
    result = 7 + result
    return result
}

@Pure
fun <!VIPER_TEXT!>whenExpressionSimple<!>(x: Int): Int {
    var y = 0
    when (x) {
        1 -> {
            y = 10
        }

        2 -> {
            return 20
        }

        else -> {
            y = 30
        }
    }
    return y + 1
}

@Pure
fun <!VIPER_TEXT!>whenNoArgumentBranching<!>(x: Int, y: Int): Int {
    when {
        x == 0 -> return 0
        x > 0 && y > 0 -> return 1
        x < 0 || y < 0 -> return -1
        else -> {
            var z = 5
            return z
        }
    }
}

@Pure
fun <!VIPER_TEXT!>stringEqualityBranching<!>(input: String): Boolean {
    var isValid = false
    if (input == "admin") {
        return true
    } else {
        if (input == "user") {
            isValid = true
        } else {
            isValid = false
        }
    }
    return isValid
}

@Pure
fun <!VIPER_TEXT!>sequentialIfWithMutation<!>(): Int {
    var x = 10
    if (x > 5) {
        x = 2
    } else {
        x = 20
    }
    if (x == 2) {
        return 100
    }
    return 0
}

@Pure
fun <!VIPER_TEXT!>complexBooleanReturn<!>(x: Int): Boolean {
    if (x > 100) {
        return true
    }
    var result = false
    if (x > 50) {
        result = true
    } else {
        if (x < 0) {
            return false
        }
        result = true
    }
    if (result) {
        return false
    } else {
        return true
    }
}

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
