// LOCALITY_CHECK_ONLY
// COMPILER_ARGUMENTS: -Xcontext-parameters

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

fun borrow(x: @Borrowed A) {}

fun share(x: A) {}

fun borrowBoth(x: @Borrowed A, y: @Borrowed A) {}

fun (@Borrowed A).borrowTarget() {}

fun A.shareTarget() {}

fun (@Borrowed A).borrowTargetAndArg(y: @Borrowed A) {}

fun `pass local as shared argument`(x: @Borrowed A) {
    share(<!LOCALITY_VIOLATION!>x<!>)
}

fun `pass global as borrowed argument`(x: A) {
    borrow(x)
}

fun `pass local as borrowed argument`(x: @Borrowed A) {
    borrow(x)
}

fun `assign local to global after passing it as borrowed argument`(x: @Borrowed A) {
    borrow(x)
    var y: A = <!LOCALITY_VIOLATION!>x<!>
}

fun `pass local twice as borrowed arguments`(x: @Borrowed A) {
    borrowBoth(x, x)
}

fun `pass global as explicit borrowed target`(x: A) {
    x.borrowTarget()
}

fun `pass local as explicit borrowed target`(x: @Borrowed A) {
    x.borrowTarget()
}

fun `pass local as explicit borrowed target and argument`(x: @Borrowed A) {
    x.borrowTargetAndArg(x)
}

fun `pass local as borrowed target`(x: @Borrowed A) {
    x.borrowTarget()
}

fun `pass global as borrowed target`(x: A) {
    x.borrowTarget()
}

fun `pass local as borrowed target and argument`(x: @Borrowed A) {
    x.borrowTargetAndArg(x)
}

fun `pass global as borrowed target and argument`(x: A) {
    x.borrowTargetAndArg(x)
}
