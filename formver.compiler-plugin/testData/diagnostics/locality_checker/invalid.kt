// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `allow locality attribute on extension receiver type`(@Borrowed x: Any) {
    fun @Borrowed Any.localReceiver() {}
    x.localReceiver()
}

fun `allow locality attribute on local variable type`() {
    val x: @Borrowed Any = Any()
}

fun `reject locality attribute on return type`(): <!INVALID_LOCALITY_TARGET!>@Borrowed Any<!> = Any()

val `reject locality attribute on top-level property type`: <!INVALID_LOCALITY_TARGET!>@Borrowed Any<!> = Any()

class `reject locality attribute on member property type` {
    val x: <!INVALID_LOCALITY_TARGET!>@Borrowed Any<!> = Any()
}
