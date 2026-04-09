package diagnostics.verification.classes

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.verify

interface Base {
    val value: Int
}

interface OtherBase {
    val tag: Int
}

class Sub1 : Base {
    override val value: Int = 1
}

class Sub2 : Base {
    override val value: Int = 2
}

class Multi : Base, OtherBase {
    override val value: Int = 10
    override val tag: Int = 20
}

fun <!VIPER_TEXT!>takeBase<!>(b: Base) {
    b.value
}

fun <!VIPER_TEXT!>takeOtherBase<!>(o: OtherBase) {
    o.tag
}

// Test 1: Subtype passed to supertype function inside if-else branches.
// Both branches create a subtype and pass it to a function expecting the supertype.
@AlwaysVerify
fun <!VIPER_TEXT!>conditionalSubtypePass<!>(flag: Boolean) {
    if (flag) {
        val s = Sub1()
        takeBase(s)
    } else {
        val s = Sub2()
        takeBase(s)
    }
}

// Test 2: Single subtype instance created before the conditional,
// then passed to supertype function in one branch only.
// The predicate must survive into the branch.
@AlwaysVerify
fun <!VIPER_TEXT!>subtypePassInOneBranch<!>(flag: Boolean) {
    val s = Sub1()
    if (flag) {
        takeBase(s)
    }
}

// Test 3: Same instance passed to two different supertype functions
// across different branches (multi-interface).
@AlwaysVerify
fun <!VIPER_TEXT!>multiInterfaceConditional<!>(flag: Boolean) {
    val m = Multi()
    if (flag) {
        takeBase(m)
    } else {
        takeOtherBase(m)
    }
}

// Test 4: Subtype created conditionally, then used after the branch.
// The join point must have the supertype predicate available.
@AlwaysVerify
fun <!VIPER_TEXT!>subtypeCreatedConditionally<!>(flag: Boolean) {
    val b: Base = if (flag) {
        Sub1()
    } else {
        Sub2()
    }
    takeBase(b)
}

// Test 5: Nested conditionals — subtype passed at increasing depth.
@AlwaysVerify
fun <!VIPER_TEXT!>nestedConditionalPass<!>(flag1: Boolean, flag2: Boolean) {
    val s = Sub1()
    if (flag1) {
        if (flag2) {
            takeBase(s)
        }
    }
}
