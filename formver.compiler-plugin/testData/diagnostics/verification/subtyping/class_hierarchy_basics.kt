// ALWAYS_VALIDATE
// Probe: basic class hierarchy verification

open class Animal(val legs: Int)
class Dog(legs: Int) : Animal(legs)

// Subtype passed to supertype parameter
fun <!VIPER_TEXT!>takesAnimal<!>(a: Animal): Int = a.legs

fun <!VIPER_TEXT!>passDogAsAnimal<!>(d: Dog): Int {
    return takesAnimal(d)
}

// Field access on subtype returns inherited field
fun <!VIPER_TEXT!>dogLegs<!>(d: Dog): Int {
    return d.legs
}
