import org.jetbrains.kotlin.formver.plugin.Manual
import org.jetbrains.kotlin.formver.plugin.NeverVerify

class ManualPermissionFields(val a: Int, @property:Manual var b: Int)

@NeverVerify // Manual fold+unfold is not implemented yet
fun <!VIPER_TEXT!>testManualPermissionFieldGetter<!>(mpf: ManualPermissionFields) {
    val a = mpf.a
    val b = mpf.b
}

@NeverVerify // Manual fold+unfold is not implemented yet
fun <!VIPER_TEXT!>testManualPermissionFieldSetter<!>(mpf: ManualPermissionFields) {
    mpf.b = 123
}
