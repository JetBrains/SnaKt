// ALWAYS_VALIDATE
// Probe: double predicate inhaling soundness
// The key concern: `as` cast inhales Bar$shared independently,
// giving us potentially two copies of nested Foo$shared.
// Does this cause unsound verification or permission errors?

open class Wrapper(val data: Int)
class ExtendedWrapper(data: Int, val tag: String) : Wrapper(data)

// This is the critical test: after cast, we inhale ExtendedWrapper$shared
// which nests Wrapper$shared. But we already have Wrapper$shared from
// the parameter. If Viper sees duplicate wildcard permissions, is that ok?
fun <!VIPER_TEXT!>castAndAccessOriginalField<!>(w: Wrapper): Int {
    val ew = w as ExtendedWrapper
    // Access field from original type (Wrapper.data) through the casted reference
    return ew.data
}

// Same but with smart cast
fun <!VIPER_TEXT!>smartCastDoubleAccess<!>(w: Wrapper): Int {
    if (w is ExtendedWrapper) {
        // We have: Wrapper$shared(w) from parameter
        // AND: ExtendedWrapper$shared(w) from smart cast (which nests Wrapper$shared)
        // Can we still unfold and access?
        return w.data + w.tag.length
    }
    return w.data
}
