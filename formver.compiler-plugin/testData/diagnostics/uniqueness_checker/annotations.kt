// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class Box(
    val a: @Unique Any = Any()
)

fun `unique parameter`(box: @Unique Box) {}

fun `borrowed parameter`(box: @Borrowed Box) {}

fun `unique borrowed parameter`(box: @Unique @Borrowed Box) {}
