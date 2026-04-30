// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `allow locality attribute on extension receiver type`(@Borrowed x: Any) {
    fun @Borrowed Any.localReceiver() {}
    x.localReceiver()
}

fun `allow locality attribute on local variable type`() {
    val x: @Borrowed Any = Any()
}

fun `reject locality attribute on return type`(): <!INVALID_LOCALITY_TYPE_TARGET!>@Borrowed Any<!> = Any()

val `reject locality attribute on top-level property type`: <!INVALID_LOCALITY_TYPE_TARGET!>@Borrowed Any<!> = Any()

class `reject locality attribute on member property type` {
    val x: <!INVALID_LOCALITY_TYPE_TARGET!>@Borrowed Any<!> = Any()
}

// Note: the assigned `listOf` expression flags an error because the locality attribute is implicitly added on the
// inferred generic parameter.
val `reject locality attribute on generic type parameter`: List<<!INVALID_LOCALITY_TYPE_TARGET!>@Borrowed Any<!>>
    = <!INVALID_LOCALITY_TYPE_TARGET!>listOf<!>()
