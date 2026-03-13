// ALWAYS_VALIDATE
// Probe: what can we reason about var fields?

import org.jetbrains.kotlin.formver.plugin.Unique

class Counter(var count: Int)

// Shared receiver: read returns havoc, so we can't reason about value
fun <!VIPER_TEXT!>sharedReadIsHavoc<!>(c: Counter): Int {
    return c.count  // expect: havoc
}

// Shared receiver: write is dropped, so this is a no-op
fun <!VIPER_TEXT!>sharedWriteIsDropped<!>(c: Counter) {
    c.count = 42  // expect: silently dropped
}

// Unique receiver: does @Unique help with reads?
fun <!VIPER_TEXT!>uniqueReadBehavior<!>(@Unique c: Counter): Int {
    return c.count  // does this still havoc?
}

// Unique receiver: does @Unique help with writes?
fun <!VIPER_TEXT!>uniqueWriteBehavior<!>(@Unique c: Counter) {
    c.count = 42  // is the write actually performed?
}

// Can we verify anything about a freshly constructed object?
fun <!VIPER_TEXT!>freshObjectVar<!>(): Int {
    val c = Counter(0)
    return c.count  // freshly constructed, can we get 0?
}
