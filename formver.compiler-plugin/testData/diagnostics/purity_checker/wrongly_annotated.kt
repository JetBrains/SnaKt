import org.jetbrains.kotlin.formver.plugin.Pure

fun <!VIPER_TEXT!>iAmAMethod<!>(): Int {
    return 1
}

<!INTERNAL_ERROR!>@Pure
fun testWronglyAnnotatedAsPure(): Int {
    return iAmAMethod()
}<!>

<!INTERNAL_ERROR!>@Pure
fun testExecutingLambda(action: () -> Int): Int {
    return action()
}<!>

class Field(var value: Int)

<!INTERNAL_ERROR!>@Pure
fun testFieldModification(field: Field): Int {
    field.value += 1
    return field.value
}<!>

fun Field.<!VIPER_TEXT!>impureExtension<!>() {
    this.value += 1
}

<!INTERNAL_ERROR!>@Pure
fun testImpureExtensionCall(field: Field): Int {
    field.impureExtension()
    return field.value
}<!>

<!INTERNAL_ERROR!>@Pure
fun Field.wronglyAnnotatedExtension() {
    this.value += 1
}<!>

class Wrapper(val field: Field)

<!INTERNAL_ERROR!>@Pure
fun testNestedFieldModification(wrapper: Wrapper): Int {
    wrapper.field.value += 1
    return wrapper.field.value
}<!>