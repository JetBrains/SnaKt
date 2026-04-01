// NEVER_VALIDATE
// FULL_VIPER_DUMP

class B()

class A(
    var unit: Unit,
    var nothing: Nothing,
    var any: Any,
    var int: Int,
    var boolean: Boolean,
    var char: Char,
    var string: String,
    var classType: B,
    var unitNull: Unit?,
    var nothingNull: Nothing?,
    var anyNull: Any?,
    var intNull: Int?,
    var booleanNull: Boolean?,
    var charNull: Char?,
    var stringNull: String?,
    var classTypeNull: B?,
    var func1: () -> Unit,
    var func2: (a: Int) -> Unit,
    var func3: () -> Int,
    var func4: (a: Int, b: Int) -> Unit,
)

fun <!VIPER_TEXT!>havoc<!>(a: A): Unit {
    var localUnit = a.unit
    var localNothing = a.nothing
    var localAny = a.any
    var localInt = a.int
    var localBoolean = a.boolean
    var localChar = a.char
    var localString = a.string
    var localClassType = a.classType
    var localUnitNull = a.unitNull
    var localNothingNull = a.nothingNull
    var localAnyNull = a.anyNull
    var localIntNull = a.intNull
    var localBooleanNull = a.booleanNull
    var localCharNull = a.charNull
    var localStringNull = a.stringNull
    var localClassTypeNull = a.classTypeNull
    var localFunc1 = a.func1
    var localFunc2 = a.func2
    var localFunc3 = a.func3
    var localFunc4 = a.func4
}
