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

fun <!VIPER_TEXT!>simpleReturnUM<!>(@Unique a: A) : B{
    return a.um
}

fun <!VIPER_TEXT!>simpleReturnUI<!>(@Unique a: A) : B {
    return a.ui
}

fun <!VIPER_TEXT!>simpleReturnSM<!>(@Unique a: A) : B {
    return a.sm
}

fun <!VIPER_TEXT!>simpleReturnSI<!>(@Unique a: A) : B {
    return a.si
}

fun <!VIPER_TEXT!>deepReturnUMSI<!>(@Unique a: A) : C {
    return a.um.si
}
