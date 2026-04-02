import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.verify

object Singleton {
    val x: Int = 10
}

@AlwaysVerify
@Suppress("USELESS_IS_CHECK")
fun <!VIPER_TEXT!>objectPropertyAccess<!>() {
    val v = Singleton.x
    verify(v is Int)
}

object Counter {
    var count: Int = 0
}

@AlwaysVerify
fun <!VIPER_TEXT!>objectMutableState<!>() {
    Counter.count = 5
    val c = Counter.count
    verify(<!VIPER_VERIFICATION_ERROR!>c == 5<!>)
}

interface Describable {
    val description: Int
}

object DescribableSingleton : Describable {
    override val description: Int = 42
}

@NeverConvert
fun readDescription(d: Describable): Int = d.description

@AlwaysVerify
@Suppress("USELESS_IS_CHECK")
fun <!VIPER_TEXT!>objectImplementsInterface<!>() {
    val d: Describable = DescribableSingleton
    val desc = d.description
    verify(desc is Int)
}

object First {
    val id: Int = 1
}

object Second {
    val id: Int = 2
}

@AlwaysVerify
@Suppress("USELESS_IS_CHECK")
fun <!VIPER_TEXT!>distinctObjects<!>() {
    val a = First
    val b = Second
    val cond1 = a is First
    val cond2 = b is Second
    verify(cond1, cond2)
}
