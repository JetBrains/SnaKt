// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

@ADT
data class RationalNumber(val num: Int, val den: Int)

@Pure
fun <!VIPER_TEXT!>wellFormed<!>(r: RationalNumber): Boolean {
    return r.den > 0
}

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>mkRationalNumber<!>(num: Int, den: Int): RationalNumber {
    preconditions { den != 0 }
    postconditions<RationalNumber> { result ->
        wellFormed(result)
    }
    return if (den > 0) RationalNumber(num, den) else RationalNumber(-num, -den)
}

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>addR<!>(a: RationalNumber, b: RationalNumber): RationalNumber {
    preconditions {
        wellFormed(a)
        wellFormed(b)
    }
    postconditions<RationalNumber> { result ->
        wellFormed(result)
    }
    return RationalNumber(a.num * b.den + b.num * a.den, a.den * b.den)
}

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>mulR<!>(a: RationalNumber, b: RationalNumber): RationalNumber {
    preconditions {
        wellFormed(a)
        wellFormed(b)
    }
    postconditions<RationalNumber> { result ->
        wellFormed(result)
    }
    return RationalNumber(a.num * b.num, a.den * b.den)
}

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>negR<!>(a: RationalNumber): RationalNumber {
    preconditions { wellFormed(a) }
    postconditions<RationalNumber> { result ->
        wellFormed(result)
    }
    return RationalNumber(-a.num, a.den)
}

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>reciprocal<!>(a: RationalNumber): RationalNumber {
    preconditions {
        wellFormed(a)
        a.num != 0
    }
    postconditions<RationalNumber> { result ->
        wellFormed(result)
    }
    return if (a.num > 0) RationalNumber(a.den, a.num) else RationalNumber(-a.den, -a.num)
}

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>eqR<!>(a: RationalNumber, b: RationalNumber): Boolean {
    preconditions {
        wellFormed(a)
        wellFormed(b)
    }
    return a.num * b.den == b.num * a.den
}

@AlwaysVerify
fun <!VIPER_TEXT!>negInvolution<!>(a: RationalNumber): Unit {
    preconditions { wellFormed(a) }
    postconditions<Unit> { _ ->
        negR(negR(a)) == a
    }
}
