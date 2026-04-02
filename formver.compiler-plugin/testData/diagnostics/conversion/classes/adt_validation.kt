// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.ADT
import org.jetbrains.kotlin.formver.plugin.NeverConvert

<!ADT_INVALID_TARGET!>@ADT
class AdtOnClass(val x: Int)<!>

@ADT
abstract class AdtOnAbstractClass

<!ADT_INVALID_TARGET!>@ADT
open class AdtOnOpenClass<!>

@ADT
object AdtOnObject {
    val x: Int = 42
}

@NeverConvert
fun takeAny(x: Any) {}

fun <!VIPER_TEXT!>useAdtObject<!>() {
    takeAny(AdtOnObject)
    takeAny(AdtOnOpenClass())
    takeAny(AdtOnClass(2))
}