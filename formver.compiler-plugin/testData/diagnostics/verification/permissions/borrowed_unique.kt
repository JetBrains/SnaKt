// ALWAYS_VALIDATE
// Probe: @Borrowed @Unique parameter behavior

import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

class Box(var value: Int)

// @Borrowed @Unique: unique permission should be returned after call
fun <!VIPER_TEXT!>borrowedUniqueRead<!>(@Borrowed @Unique b: Box): Int {
    return b.value
}

// @Unique without @Borrowed: permission is consumed
fun <!VIPER_TEXT!>consumedUniqueRead<!>(@Unique b: Box): Int {
    return b.value
}

// Call a borrowed-unique function and then use the value again
fun <!VIPER_TEXT!>callBorrowedThenUse<!>(@Unique b: Box): Int {
    val x = borrowedUniqueRead(b)
    val y = borrowedUniqueRead(b)  // should work: permission returned
    return x + y
}
