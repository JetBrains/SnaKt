// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.*

class Inner(var v: Int)
class Outer(@Unique var inner: Inner)
class NullableOuter(@Unique var inner: Inner?)

@Pure
fun <!VIPER_TEXT!>sumInner<!>(@Unique @Borrowed x: Inner, @Unique @Borrowed y: Inner): Int {
    return x.v + y.v
}

@Pure
fun <!VIPER_TEXT!>chain<!>(@Unique @Borrowed a: Outer): Int {
    return a.inner.v
}

@Pure
fun <!VIPER_TEXT!>branchThenField<!>(@Unique @Borrowed a: Outer, @Unique @Borrowed b: Outer, p: Boolean): Int {
    val x = if (p) a.inner else b.inner
    return x.v
}

@Pure
fun <!VIPER_TEXT!>reassignThenField<!>(@Unique @Borrowed a: Outer, @Unique @Borrowed b: Outer, p: Boolean): Int {
    var x = a.inner
    if (p) {
        x = b.inner
    }
    return x.v
}

@Pure
fun <!VIPER_TEXT!>independentBranches<!>(
    @Unique @Borrowed a: Outer,
    @Unique @Borrowed b: Outer,
    @Unique @Borrowed c: Outer,
    @Unique @Borrowed d: Outer,
    p: Boolean,
    q: Boolean,
): Int {
    val x = if (p) a.inner else b.inner
    val y = if (q) c.inner else d.inner
    val result = sumInner(x, y)
    return result
}

@Pure
fun <!VIPER_TEXT!>independentBranchesReturned<!>(
    @Unique @Borrowed a: Outer,
    @Unique @Borrowed b: Outer,
    @Unique @Borrowed c: Outer,
    @Unique @Borrowed d: Outer,
    p: Boolean,
    q: Boolean,
): Int {
    val x = if (p) a.inner else b.inner
    val y = if (q) c.inner else d.inner
    return sumInner(x, y)
}

@Pure
fun <!VIPER_TEXT!>elvisThenSafeField<!>(@Unique @Borrowed a: NullableOuter, @Unique @Borrowed b: Outer): Int {
    return (a.inner ?: b.inner).v
}
