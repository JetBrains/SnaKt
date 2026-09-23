// LOCALITY_CHECK_ONLY
// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.Borrowed

class LocalFunctionValue

fun borrowLocalFunctionValue(x: @Borrowed LocalFunctionValue) {}

fun `call local function after declaration`(x: @Borrowed LocalFunctionValue) {
    fun local() {
        borrowLocalFunctionValue(x)
    }

    local()
}

fun `recursive local function preserves borrowed capture`(x: @Borrowed LocalFunctionValue) {
    fun recursive(n: Int) {
        if (n == 0) {
            borrowLocalFunctionValue(x)
        } else {
            recursive(n - 1)
        }
    }

    recursive(1)
}

fun `nested local function captures outer parameter`(x: @Borrowed LocalFunctionValue) {
    fun outer() {
        fun inner() {
            borrowLocalFunctionValue(x)
        }

        inner()
    }

    outer()
}

fun `nested local function can shadow outer name`(x: @Borrowed LocalFunctionValue) {
    fun use() {}

    fun nested() {
        fun use() {
            borrowLocalFunctionValue(x)
        }

        use()
    }

    use()
    nested()
}
