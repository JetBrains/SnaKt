// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `assign merged local owners to global`(x: @Borrowed Any) {
    { y: @Borrowed Any ->
        var z: Any = <!LOCALITY_VIOLATION!>if (false) { x } else { y }<!>
    }
}
