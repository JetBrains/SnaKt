@file:Suppress("USELESS_IS_CHECK")

// ALWAYS_VALIDATE
// Probe: abstract class subtyping verification

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

abstract class Vehicle(val wheels: Int)
class Car : Vehicle(4)
class Bicycle : Vehicle(2)

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>carIsVehicle<!>(c: Car): Boolean {
    contract {
        returns(true)
    }
    return c is Vehicle
}

// Access inherited field from abstract class
fun <!VIPER_TEXT!>getWheels<!>(v: Vehicle): Int {
    return v.wheels
}
