// LOCALITY_CHECK_ONLY
// LANGUAGE: +ContextParameters

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

fun `allow locality attribute on extension receiver type`(@Borrowed x: Any) {
    fun @Borrowed Any.localReceiver() {}
    x.localReceiver()
}

fun `allow locality attribute on local variable type`() {
    val x: @Borrowed Any = Any()
}

fun `allow locality attribute on both function type and its parameter type`(f: @Borrowed (@Borrowed Any) -> Unit) {}

fun `allow locality attribute on both function type and its nested parameter type`(f: @Borrowed ((@Borrowed Any) -> Unit) -> Unit) {}

fun `allow locality attribute on both function type and its receiver type`(f: @Borrowed ((@Borrowed Any).() -> Unit)) {}

fun `allow locality attribute on both function type and its context parameter type`(f: @Borrowed context(@Borrowed A) () -> Unit) {}

fun `reject locality attribute on return type`(): <!INVALID_LOCALITY_TYPE_TARGET!>@Borrowed Any<!> = Any()

val `reject locality attribute on top-level property type`: <!INVALID_LOCALITY_TYPE_TARGET!>@Borrowed Any<!> = Any()

class `reject locality attribute on member property type` {
    val x: <!INVALID_LOCALITY_TYPE_TARGET!>@Borrowed Any<!> = Any()
}

val `reject locality attribute on generic type parameter`: List<<!INVALID_LOCALITY_TYPE_TARGET!>@Borrowed Any<!>>
    = listOf()

typealias `Invalid Type` = <!INVALID_LOCALITY_TYPE_TARGET!>@Borrowed Any<!>
