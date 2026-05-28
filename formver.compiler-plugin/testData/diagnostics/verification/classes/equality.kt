// FULL_JDK
// FULL_VIPER_DUMP

import org.jetbrains.kotlin.formver.plugin.*

class NoEquals

class CustomEquals {
    override fun <!VIPER_TEXT!>equals<!>(other: Any?): Boolean {
        preconditions { (true || false) && true }
        return true
    }
}

class CustomPureEquals {
    @Pure
    override fun <!VIPER_TEXT!>equals<!>(other: Any?): Boolean {
        return true
    }
}

open class OpenClassWithoutEquals {}

open class OpenClassWithEquals {
    override fun <!VIPER_TEXT!>equals<!>(other: Any?): Boolean {
        postconditions<Boolean> { it == true }
        return true
    }
}

class UseRefEquality {
    override fun <!VIPER_TEXT!>equals<!>(other: Any?): Boolean {
        return this === other
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>useNoEquals<!>(o1: NoEquals, o2: NoEquals): Boolean = o1 == o2 && o1.equals(o2)

@AlwaysVerify
fun <!VIPER_TEXT!>useCustomEquals<!>(o1: CustomEquals, o2: CustomEquals): Boolean = o1 == o2

@AlwaysVerify
fun <!VIPER_TEXT!>useCustomPureEquals<!>(o1: CustomPureEquals, o2: CustomPureEquals): Boolean = o1 == o2

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>useCustomPureEqualsInPure<!>(o1: CustomPureEquals, o2: CustomPureEquals): Boolean = o1 == o2

<!PURITY_VIOLATION!>@AlwaysVerify
@Pure
fun testPurityViolationByEquals(o1: CustomEquals, o2: CustomEquals): Boolean = o1 == o2<!>

// Although the method is open, it we use the statically dispatched method. This is a known limitation
@AlwaysVerify
fun <!VIPER_TEXT!>useOpenClassWithoutEquals<!>(o1: OpenClassWithoutEquals, o2: OpenClassWithoutEquals): Boolean = o1 == o2

@AlwaysVerify
fun <!VIPER_TEXT!>useOpenClassWithEquals<!>(o1: OpenClassWithEquals, o2: OpenClassWithEquals): Boolean = o1 == o2

@ADT
data class PairWithPrimitives(val a: Int, val b: Int)

@AlwaysVerify
fun <!VIPER_TEXT!>usePairWithPrimitives<!>(p1: PairWithPrimitives, p2: PairWithPrimitives): Boolean = p1 == p2

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>usePairWithPrimitivesPure<!>(p1: PairWithPrimitives, p2: PairWithPrimitives): Boolean = p1 == p2

@ADT
data class BoxWithCustomEquals(val a: CustomEquals)

@AlwaysVerify
fun <!VIPER_TEXT!>usePairWithBoxWithCustomEquals<!>(b1: BoxWithCustomEquals, b2: BoxWithCustomEquals): Boolean = b1 == b2

<!PURITY_VIOLATION!>@AlwaysVerify
@Pure
fun usePairWithBoxWithCustomEqualsPure(b1: BoxWithCustomEquals, b2: BoxWithCustomEquals): Boolean = b1 == b2<!>

@ADT
data class BoxWithCustomPureEquals(val a: CustomPureEquals)

@AlwaysVerify
fun <!VIPER_TEXT!>usePairWithBoxWithCustomPureEquals<!>(b1: BoxWithCustomPureEquals, b2: BoxWithCustomPureEquals): Boolean = b1 == b2

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>usePairWithBoxWithCustomPureEqualsPure<!>(b1: BoxWithCustomPureEquals, b2: BoxWithCustomPureEquals): Boolean = b1 == b2

@AlwaysVerify
fun <!VIPER_TEXT!>testRefEquality<!>(o1: NoEquals, o2: NoEquals): Boolean = o1 === o2

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>testCompareInts<!>(x: Int, y: Int): Boolean = x == y && x.equals(y)

@ADT
data object Red

@ADT
data object Blue

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>useRedAndBlue<!>(r: Red, b: Blue): Boolean = r == b

@AlwaysVerify
fun <!VIPER_TEXT!>testBlueBlueComparision<!>(b1: Blue, b2: Blue): Boolean {
    postconditions<Boolean> { it }
    return b1 == b2
}

@AlwaysVerify
fun <!VIPER_TEXT!>testRedBlueComparision<!>(r: Red, b: Blue): Boolean {
    postconditions<Boolean> { it }
    return r != b
}

@ADT
sealed interface LinkedListSum
@ADT
data class Node(val head: Int, val tail: LinkedListSum) : LinkedListSum
@ADT
data object Nil : LinkedListSum

@AlwaysVerify
fun <!VIPER_TEXT!>testLLComparisionsCoarse<!>(ll1: LinkedListSum, ll2: LinkedListSum): Boolean = ll1 == ll2

@AlwaysVerify
fun <!VIPER_TEXT!>testLLComparisionsGranular<!>(ll1: LinkedListSum, ll2: LinkedListSum): Boolean = when (ll1) {
    is Node -> ll1 == ll2
    is Nil -> ll1 == ll2
}

@ADT
data class NodeLL(val head: Int, val tail: NodeLL?)

@AlwaysVerify
fun <!VIPER_TEXT!>testNodeLLComparisionsCoarse<!>(ll1: NodeLL, ll2: NodeLL): Boolean = ll1 == ll2
