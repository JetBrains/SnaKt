// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.Pure

fun <!VIPER_TEXT!>iAmAMethod<!>(): Int {
    return 1
}

<!PURITY_VIOLATION!>@Pure
fun <!VERIFICATION_SKIPPED!>testWronglyAnnotatedAsPure<!>(): Int {
    return iAmAMethod()
}<!>

<!PURITY_VIOLATION!>@Pure
fun <!VERIFICATION_SKIPPED!>testExecutingLambda<!>(action: () -> Int): Int {
    return action()
}<!>

class Field(var value: Int)

<!PURITY_VIOLATION!>@Pure
fun <!VERIFICATION_SKIPPED!>testFieldModification<!>(field: Field): Int {
    field.value += 1
    return field.value
}<!>

fun Field.<!VIPER_TEXT!>impureExtension<!>() {
    this.value += 1
}

<!PURITY_VIOLATION!>@Pure
fun <!VERIFICATION_SKIPPED!>testImpureExtensionCall<!>(field: Field): Int {
    field.impureExtension()
    return field.value
}<!>

<!PURITY_VIOLATION!>@Pure
fun Field.<!VERIFICATION_SKIPPED!>wronglyAnnotatedExtension<!>() {
    this.value += 1
}<!>

class Wrapper(val field: Field)

<!PURITY_VIOLATION!>@Pure
fun <!VERIFICATION_SKIPPED!>testNestedFieldModification<!>(wrapper: Wrapper): Int {
    wrapper.field.value += 1
    return wrapper.field.value
}<!>
