// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `capture local from global lambda capturing`(x: @Borrowed Any) {
    {
        var y = <!INVALID_LOCALITY_CAPTURE!>x<!>
    }
}

fun `capture local from nested global lambda`(x: @Borrowed Any) {
    {
        {
            var y = <!INVALID_LOCALITY_CAPTURE!>x<!>
        }
    }
}

fun `capture local from doubly nested local lambda`(x: @Borrowed Any) {
    val f: @Borrowed () -> Unit = {
        val g: @Borrowed () -> Unit = {
            var y = <!INVALID_LOCALITY_CAPTURE!>x<!>
        }
    }
}

fun `capture local from nested local lambda`(x: @Borrowed Any) {
    val f: @Borrowed () -> Unit = {
        var y = <!INVALID_LOCALITY_CAPTURE!>x<!>
    }
}
