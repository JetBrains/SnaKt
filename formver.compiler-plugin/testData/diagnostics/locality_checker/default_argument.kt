// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `assign global value as global default argument`(
    x: Any = Any()
) {}

fun `assign global value as local default argument`(
    @Borrowed x: Any = Any()
) {}

fun `assign local argument as global default argument`(
    @Borrowed x: Any,
    y: Any = <!LOCALITY_VIOLATION!>x<!>
) {}

fun `assign local argument as local default argument`(
    @Borrowed x: Any,
    @Borrowed y: Any = x
) {}

class `assign global value as global default argument in constructor`(
    @Borrowed x: Any = Any(),
)

class `assign local argument as global default argument in constructor`(
    @Borrowed x: Any,
    y: Any = <!LOCALITY_VIOLATION!>x<!>,
)
