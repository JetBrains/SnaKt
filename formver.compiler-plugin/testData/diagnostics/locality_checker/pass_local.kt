// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

fun borrow(@Borrowed x: A) {}

fun share(x: A) {}

fun borrowBoth(@Borrowed x: A, @Borrowed y: A) {}

fun shareAndBorrow(x: A, @Borrowed y: A) {}

fun borrowWithDefault(@Borrowed x: A = A()) {}

fun borrowDefaultAndShare(@Borrowed x: A = A(), y: A) {}

fun @receiver:Borrowed A.borrowTarget() {}

fun A.shareTarget() {}

fun @receiver:Borrowed A.borrowTargetAndArg(@Borrowed y: A) {}

fun `pass local as shared argument`(@Borrowed x: A) {
    share(<!LOCALITY_VIOLATION!>x<!>)
}

fun `pass global as borrowed argument`(x: A) {
    borrow(x)
}

fun `pass global as named borrowed argument`(x: A) {
    borrow(x=x)
}

fun `pass local as borrowed argument`(@Borrowed x: A) {
    borrow(x)
}

fun `pass local as named borrowed argument`(@Borrowed x: A) {
    borrow(x=x)
}

fun `assign local to global after passing it as borrowed argument`(@Borrowed x: A) {
    borrow(x)
    var y: A = <!LOCALITY_VIOLATION!>x<!>
}

fun `share local after initializing it with a global value`(x: A) {
    @Borrowed var y = x
    share(<!LOCALITY_VIOLATION!>y<!>)
}

fun `pass local twice as borrowed arguments`(@Borrowed x: A) {
    borrowBoth(x, x)
}

fun `pass local twice as named borrowed arguments`(@Borrowed x: A) {
    borrowBoth(y=x, x=x)
}

fun `pass local as explicit shared target`(@Borrowed x: A) {
    <!LOCALITY_VIOLATION!>x<!>.shareTarget()
}

fun `pass global as explicit borrowed target`(x: A) {
    x.borrowTarget()
}

fun `pass local as explicit borrowed target`(@Borrowed x: A) {
    x.borrowTarget()
}

fun `pass local as explicit borrowed target and argument`(@Borrowed x: A) {
    x.borrowTargetAndArg(x)
}

fun `pass local as borrowed target`(@Borrowed x: A) {
    x.borrowTarget()
}

fun `pass global as borrowed target`(x: A) {
    x.borrowTarget()
}

fun `pass local as borrowed target and argument`(@Borrowed x: A) {
    x.borrowTargetAndArg(x)
}

fun `pass global as borrowed target and argument`(x: A) {
    x.borrowTargetAndArg(x)
}

fun `pass local as named local and global arguments`(@Borrowed x: A) {
    shareAndBorrow(y = x, x = <!LOCALITY_VIOLATION!>x<!>)
}

fun `pass local as named local along local default`(@Borrowed x: A) {
    borrowDefaultAndShare(y = <!LOCALITY_VIOLATION!>x<!>)
}

fun @receiver:Borrowed A.`pass local as implicit shared target`() {
    <!LOCALITY_VIOLATION!>shareTarget()<!>
}

fun @receiver:Borrowed A.`pass local as implicit local target`() {
    borrowTarget()
}

fun @receiver:Borrowed A.`pass local this as explicit shared target`() {
    <!LOCALITY_VIOLATION!>this<!>.shareTarget()
}

fun @receiver:Borrowed A.`pass local this as explicit local target`() {
    this.borrowTarget()
}
