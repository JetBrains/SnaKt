// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class Box(@Unique val a: Any)

fun `unique parameter`(@Unique box: Box) {}

fun `borrowed parameter`(@Borrowed box: Box) {}

fun `unique borrowed parameter`(@Unique @Borrowed box: Box) {}

