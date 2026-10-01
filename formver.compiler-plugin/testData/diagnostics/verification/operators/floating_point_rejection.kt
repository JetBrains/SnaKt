// FULL_JDK

fun <!VIPER_TEXT!>integerArithmeticControl<!>(): Int = 1 + 2

fun doubleLiteral(): Double = <!INTERNAL_ERROR!>1.5<!>

fun floatLiteral(): Float = <!INTERNAL_ERROR!>1.5f<!>

fun doubleArithmetic(): Double = <!INTERNAL_ERROR!>1.5<!> + 2.25

fun floatArithmetic(): Float = <!INTERNAL_ERROR!>1.5f<!> * 2.0f

fun doubleComparison(): Boolean = <!INTERNAL_ERROR!>1.5<!> < 2.0

fun floatEquality(): Boolean = <!INTERNAL_ERROR!>1.5f<!> == 1.5f

fun doubleNaN(): Double = <!INTERNAL_ERROR!>Double<!>.NaN

fun floatNaN(): Float = <!INTERNAL_ERROR!>Float<!>.NaN

fun positiveInfinity(): Double = <!INTERNAL_ERROR!>Double<!>.POSITIVE_INFINITY

fun negativeInfinity(): Float = <!INTERNAL_ERROR!>Float<!>.NEGATIVE_INFINITY
