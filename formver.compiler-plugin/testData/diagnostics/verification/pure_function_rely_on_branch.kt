import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
@Pure
fun safeDivide(x: Int, y: Int): Int {
    var res = 0
    if (y != 0) {
        res = x / y
    }
    return res
}

@AlwaysVerify
@Pure
fun getStringLength(obj: Any): Int {
    var len = -1
    if (obj is String) {
        len = obj.length
    }
    return len
}

@AlwaysVerify
@Pure
fun safeNestedDivide(x: Int, y: Int, z: Int): Int {
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
fun safeInverseDifference(x: Int, y: Int): Int {
    var res = 0
    if (x != y) {
        res = 100 / (x - y)
    }
    return res
}