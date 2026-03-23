import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

class A(
    @Unique var um: B,  // unique-mutable
    @Unique val ui: B,  // unique-immutable
    var sm: B,          // shared-mutable
    val si: B,          // shared-immutable
)


class B(
    @Unique var um: C,  // unique-mutable
    @Unique val ui: C,  // unique-immutable
    var sm: C,          // shared-mutable
    val si: C,          // shared-immutable
)


class C(
    @Unique var um: Any,  // unique-mutable
    @Unique val ui: Any,  // unique-immutable
    var sm: Any,          // shared-mutable
    val si: Any,          // shared-immutable
)


fun <!VIPER_TEXT!>um<!>(@Unique a: A) {
    var x = a.um
}

fun <!VIPER_TEXT!>ui<!>(@Unique a: A) {
    var x = a.ui
}

fun <!VIPER_TEXT!>sm<!>(@Unique a: A) {
    var x = a.sm
}

fun <!VIPER_TEXT!>si<!>(@Unique a: A) {
    var x = a.si
}

fun <!VIPER_TEXT!>um_um<!>(@Unique a: A) {
    var x = a.um.um
}

fun <!VIPER_TEXT!>um_ui<!>(@Unique a: A) {
    var x = a.um.ui
}

fun <!VIPER_TEXT!>um_sm<!>(@Unique a: A) {
    var x = a.um.sm
}

fun <!VIPER_TEXT!>um_si<!>(@Unique a: A) {
    var x = a.um.si
}

fun <!VIPER_TEXT!>ui_um<!>(@Unique a: A) {
    var x = a.ui.um
}

fun <!VIPER_TEXT!>ui_ui<!>(@Unique a: A) {
    var x = a.ui.ui
}

fun <!VIPER_TEXT!>ui_sm<!>(@Unique a: A) {
    var x = a.ui.sm
}

fun <!VIPER_TEXT!>ui_si<!>(@Unique a: A) {
    var x = a.ui.si
}

fun <!VIPER_TEXT!>sm_um<!>(@Unique a: A) {
    var x = a.sm.um
}

fun <!VIPER_TEXT!>sm_ui<!>(@Unique a: A) {
    var x = a.sm.ui
}

fun <!VIPER_TEXT!>sm_sm<!>(@Unique a: A) {
    var x = a.sm.sm
}

fun <!VIPER_TEXT!>sm_si<!>(@Unique a: A) {
    var x = a.sm.si
}

fun <!VIPER_TEXT!>si_um<!>(@Unique a: A) {
    var x = a.si.um
}

fun <!VIPER_TEXT!>si_ui<!>(@Unique a: A) {
    var x = a.si.ui
}

fun <!VIPER_TEXT!>si_sm<!>(@Unique a: A) {
    var x = a.si.sm
}

fun <!VIPER_TEXT!>si_si<!>(@Unique a: A) {
    var x = a.si.si
}


fun <!VIPER_TEXT!>um_um_um<!>(@Unique a: A) {
    var x = a.um.um.um
}

fun <!VIPER_TEXT!>um_um_ui<!>(@Unique a: A) {
    var x = a.um.um.ui
}

fun <!VIPER_TEXT!>um_um_sm<!>(@Unique a: A) {
    var x = a.um.um.sm
}

fun <!VIPER_TEXT!>um_um_si<!>(@Unique a: A) {
    var x = a.um.um.si
}

fun <!VIPER_TEXT!>um_ui_um<!>(@Unique a: A) {
    var x = a.um.ui.um
}

fun <!VIPER_TEXT!>um_ui_ui<!>(@Unique a: A) {
    var x = a.um.ui.ui
}

fun <!VIPER_TEXT!>um_ui_sm<!>(@Unique a: A) {
    var x = a.um.ui.sm
}

fun <!VIPER_TEXT!>um_ui_si<!>(@Unique a: A) {
    var x = a.um.ui.si
}

fun <!VIPER_TEXT!>um_sm_um<!>(@Unique a: A) {
    var x = a.um.sm.um
}

fun <!VIPER_TEXT!>um_sm_ui<!>(@Unique a: A) {
    var x = a.um.sm.ui
}

fun <!VIPER_TEXT!>um_sm_sm<!>(@Unique a: A) {
    var x = a.um.sm.sm
}

fun <!VIPER_TEXT!>um_sm_si<!>(@Unique a: A) {
    var x = a.um.sm.si
}

fun <!VIPER_TEXT!>um_si_um<!>(@Unique a: A) {
    var x = a.um.si.um
}

fun <!VIPER_TEXT!>um_si_ui<!>(@Unique a: A) {
    var x = a.um.si.ui
}

fun <!VIPER_TEXT!>um_si_sm<!>(@Unique a: A) {
    var x = a.um.si.sm
}

fun <!VIPER_TEXT!>um_si_si<!>(@Unique a: A) {
    var x = a.um.si.si
}


fun <!VIPER_TEXT!>ui_um_um<!>(@Unique a: A) {
    var x = a.ui.um.um
}

fun <!VIPER_TEXT!>ui_um_ui<!>(@Unique a: A) {
    var x = a.ui.um.ui
}

fun <!VIPER_TEXT!>ui_um_sm<!>(@Unique a: A) {
    var x = a.ui.um.sm
}

fun <!VIPER_TEXT!>ui_um_si<!>(@Unique a: A) {
    var x = a.ui.um.si
}

fun <!VIPER_TEXT!>ui_ui_um<!>(@Unique a: A) {
    var x = a.ui.ui.um
}

fun <!VIPER_TEXT!>ui_ui_ui<!>(@Unique a: A) {
    var x = a.ui.ui.ui
}

fun <!VIPER_TEXT!>ui_ui_sm<!>(@Unique a: A) {
    var x = a.ui.ui.sm
}

fun <!VIPER_TEXT!>ui_ui_si<!>(@Unique a: A) {
    var x = a.ui.ui.si
}

fun <!VIPER_TEXT!>ui_sm_um<!>(@Unique a: A) {
    var x = a.ui.sm.um
}

fun <!VIPER_TEXT!>ui_sm_ui<!>(@Unique a: A) {
    var x = a.ui.sm.ui
}

fun <!VIPER_TEXT!>ui_sm_sm<!>(@Unique a: A) {
    var x = a.ui.sm.sm
}

fun <!VIPER_TEXT!>ui_sm_si<!>(@Unique a: A) {
    var x = a.ui.sm.si
}

fun <!VIPER_TEXT!>ui_si_um<!>(@Unique a: A) {
    var x = a.ui.si.um
}

fun <!VIPER_TEXT!>ui_si_ui<!>(@Unique a: A) {
    var x = a.ui.si.ui
}

fun <!VIPER_TEXT!>ui_si_sm<!>(@Unique a: A) {
    var x = a.ui.si.sm
}

fun <!VIPER_TEXT!>ui_si_si<!>(@Unique a: A) {
    var x = a.ui.si.si
}


fun <!VIPER_TEXT!>sm_um_um<!>(@Unique a: A) {
    var x = a.sm.um.um
}

fun <!VIPER_TEXT!>sm_um_ui<!>(@Unique a: A) {
    var x = a.sm.um.ui
}

fun <!VIPER_TEXT!>sm_um_sm<!>(@Unique a: A) {
    var x = a.sm.um.sm
}

fun <!VIPER_TEXT!>sm_um_si<!>(@Unique a: A) {
    var x = a.sm.um.si
}

fun <!VIPER_TEXT!>sm_ui_um<!>(@Unique a: A) {
    var x = a.sm.ui.um
}

fun <!VIPER_TEXT!>sm_ui_ui<!>(@Unique a: A) {
    var x = a.sm.ui.ui
}

fun <!VIPER_TEXT!>sm_ui_sm<!>(@Unique a: A) {
    var x = a.sm.ui.sm
}

fun <!VIPER_TEXT!>sm_ui_si<!>(@Unique a: A) {
    var x = a.sm.ui.si
}

fun <!VIPER_TEXT!>sm_sm_um<!>(@Unique a: A) {
    var x = a.sm.sm.um
}

fun <!VIPER_TEXT!>sm_sm_ui<!>(@Unique a: A) {
    var x = a.sm.sm.ui
}

fun <!VIPER_TEXT!>sm_sm_sm<!>(@Unique a: A) {
    var x = a.sm.sm.sm
}

fun <!VIPER_TEXT!>sm_sm_si<!>(@Unique a: A) {
    var x = a.sm.sm.si
}

fun <!VIPER_TEXT!>sm_si_um<!>(@Unique a: A) {
    var x = a.sm.si.um
}

fun <!VIPER_TEXT!>sm_si_ui<!>(@Unique a: A) {
    var x = a.sm.si.ui
}

fun <!VIPER_TEXT!>sm_si_sm<!>(@Unique a: A) {
    var x = a.sm.si.sm
}

fun <!VIPER_TEXT!>sm_si_si<!>(@Unique a: A) {
    var x = a.sm.si.si
}


fun <!VIPER_TEXT!>si_um_um<!>(@Unique a: A) {
    var x = a.si.um.um
}

fun <!VIPER_TEXT!>si_um_ui<!>(@Unique a: A) {
    var x = a.si.um.ui
}

fun <!VIPER_TEXT!>si_um_sm<!>(@Unique a: A) {
    var x = a.si.um.sm
}

fun <!VIPER_TEXT!>si_um_si<!>(@Unique a: A) {
    var x = a.si.um.si
}

fun <!VIPER_TEXT!>si_ui_um<!>(@Unique a: A) {
    var x = a.si.ui.um
}

fun <!VIPER_TEXT!>si_ui_ui<!>(@Unique a: A) {
    var x = a.si.ui.ui
}

fun <!VIPER_TEXT!>si_ui_sm<!>(@Unique a: A) {
    var x = a.si.ui.sm
}

fun <!VIPER_TEXT!>si_ui_si<!>(@Unique a: A) {
    var x = a.si.ui.si
}

fun <!VIPER_TEXT!>si_sm_um<!>(@Unique a: A) {
    var x = a.si.sm.um
}

fun <!VIPER_TEXT!>si_sm_ui<!>(@Unique a: A) {
    var x = a.si.sm.ui
}

fun <!VIPER_TEXT!>si_sm_sm<!>(@Unique a: A) {
    var x = a.si.sm.sm
}

fun <!VIPER_TEXT!>si_sm_si<!>(@Unique a: A) {
    var x = a.si.sm.si
}

fun <!VIPER_TEXT!>si_si_um<!>(@Unique a: A) {
    var x = a.si.si.um
}

fun <!VIPER_TEXT!>si_si_ui<!>(@Unique a: A) {
    var x = a.si.si.ui
}

fun <!VIPER_TEXT!>si_si_sm<!>(@Unique a: A) {
    var x = a.si.si.sm
}

fun <!VIPER_TEXT!>si_si_si<!>(@Unique a: A) {
    var x = a.si.si.si
}