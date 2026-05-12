import org.jetbrains.kotlin.formver.plugin.Manual
import org.jetbrains.kotlin.formver.plugin.NeverVerify

class ManualPermissionFields(@Manual val a: Int, @Manual var b: Int)

// Manual fold+unfold is not implemented yet
<!VIPER_VERIFICATION_ERROR!>fun <!VIPER_TEXT!>testManualPermissionFieldGetter<!>(mpf: ManualPermissionFields) {
    val a = mpf.a
    val b = mpf.b
}<!>

// Manual fold+unfold is not implemented yet
fun <!VIPER_TEXT!>testManualPermissionFieldSetter<!>(mpf: ManualPermissionFields) {
    <!VIPER_VERIFICATION_ERROR!>mpf.b = 123<!>
}
