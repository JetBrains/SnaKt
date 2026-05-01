// CHECK_UNIQUENESS
// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

class X(var v: Int)

// A write through a unique receiver should propagate to a subsequent read of the same field.
@AlwaysVerify
fun <!VIPER_TEXT!>writeThenRead<!>(@Unique x: X) {
    x.v = 7
    val k = x.v
    verify(k == 7)
}

// Last-write-wins: the most recent write should be visible on read.
@AlwaysVerify
fun <!VIPER_TEXT!>writeTwiceReadOnce<!>(@Unique x: X) {
    x.v = 1
    x.v = 9
    val k = x.v
    verify(k == 9)
}

// A read interleaved between two writes should reflect the prior write, and the post-write
// read should reflect the new write.
@AlwaysVerify
fun <!VIPER_TEXT!>readWriteRead<!>(@Unique x: X) {
    x.v = 3
    val k1 = x.v
    x.v = 11
    val k2 = x.v
    verify(k1 == 3)
    verify(k2 == 11)
}

// Two unique receivers: a write to one must not affect a read of another.
@AlwaysVerify
fun <!VIPER_TEXT!>writeOneReadAnother<!>(@Unique x: X, @Unique y: X) {
    x.v = 5
    y.v = 8
    verify(x.v == 5)
    verify(y.v == 8)
}
