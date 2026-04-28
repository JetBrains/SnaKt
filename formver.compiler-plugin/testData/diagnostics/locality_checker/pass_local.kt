// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

fun borrow(x: @Borrowed A) {}

fun share(x: A) {}

fun borrowBoth(x: @Borrowed A, y: @Borrowed A) {}

fun shareAndBorrow(x: A, y: @Borrowed A) {}

fun borrowWithDefault(x: @Borrowed A = A()) {}

fun borrowDefaultAndShare(x: @Borrowed A = A(), y: A) {}

fun @Borrowed A.borrowTarget() {}

fun A.shareTarget() {}

fun @Borrowed A.borrowTargetAndArg(y: @Borrowed A) {}

fun `pass local as shared argument`(x: @Borrowed A) {
    share(<!LOCALITY_VIOLATION!>x<!>)
}

fun `pass global as borrowed argument`(x: A) {
    borrow(x)
}

fun `pass global as named borrowed argument`(x: A) {
    borrow(x=x)
}

fun `pass local as borrowed argument`(x: @Borrowed A) {
    borrow(x)
}

fun `pass local as named borrowed argument`(x: @Borrowed A) {
    borrow(x=x)
}

fun `assign local to global after passing it as borrowed argument`(x: @Borrowed A) {
    borrow(x)
    var y: A = <!LOCALITY_VIOLATION!>x<!>
}

fun `share local after initializing it with a global value`(x: A) {
    var y: @Borrowed A = x
    share(<!LOCALITY_VIOLATION!>y<!>)
}

fun `pass local twice as borrowed arguments`(x: @Borrowed A) {
    borrowBoth(x, x)
}

fun `pass local twice as named borrowed arguments`(x: @Borrowed A) {
    borrowBoth(y=x, x=x)
}

fun `pass local as explicit shared target`(x: @Borrowed A) {
    <!LOCALITY_VIOLATION!>x<!>.shareTarget()
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

fun `pass local as named local and global arguments`(x: @Borrowed A) {
    shareAndBorrow(y = x, x = <!LOCALITY_VIOLATION!>x<!>)
}

fun `pass local as named local along local default`(x: @Borrowed A) {
    borrowDefaultAndShare(y = <!LOCALITY_VIOLATION!>x<!>)
}

fun @Borrowed A.`pass local as implicit shared target`() {
    <!LOCALITY_VIOLATION!>shareTarget()<!>
}

fun @Borrowed A.`pass local as implicit local target`() {
    borrowTarget()
}

fun @Borrowed A.`pass local this as explicit shared target`() {
    <!LOCALITY_VIOLATION!>this<!>.shareTarget()
}

fun @Borrowed A.`pass local this as explicit local target`() {
    this.borrowTarget()
}

fun `pass local as local argument in local property initializer`(x: @Borrowed A) {
    val y = borrow(x)
}

fun `pass outer local as local argument in lambda`(x: @Borrowed A) {
    {
        borrow(<!LOCALITY_VIOLATION!>x<!>)
    }
}
open class `base with global constructor parameter`(x: Any)

class `derive and call super` : `base with global constructor parameter` {
    constructor(x: Any, marker: Int) : super(x)

    constructor(x: @Borrowed Any) : super(<!LOCALITY_VIOLATION!>x<!>)
}

class `chain this constructors with global parameter` {
    constructor(x: Any)

    constructor(x: @Borrowed Any, marker: Int) : this(<!LOCALITY_VIOLATION!>x<!>)
}

open class `base with borrowed constructor parameter`(x: @Borrowed Any)

class `derive and call borrowed super` : `base with borrowed constructor parameter` {
    constructor(x: @Borrowed Any) : super(x)
}

class `chain this constructors with borrowed parameter` {
    constructor(x: @Borrowed Any)

    constructor(x: @Borrowed Any, marker: Int) : this(x)
}
