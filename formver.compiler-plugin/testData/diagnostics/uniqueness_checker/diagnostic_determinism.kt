// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class DeterminismBox {
    var value: @Unique Any = Any()
}

fun consumeDeterministically(value: @Unique Any) {}

fun borrowDeterministically(value: @Borrowed Any) {}

// Positive control: borrowing leaves the unique value available.
fun `borrowing preserves source`(value: @Unique Any) {
    borrowDeterministically(value)
    consumeDeterministically(value)
}

fun `compact formatting reports moved accesses in source order`(first: @Unique Any, second: @Unique Any) {
    consumeDeterministically(first); consumeDeterministically(second)
    consumeDeterministically(<!INVALID_MOVED_ACCESS!>first<!>); consumeDeterministically(<!INVALID_MOVED_ACCESS!>second<!>)
}

fun `expanded formatting reports moved accesses in source order`(
    first: @Unique Any,
    second: @Unique Any,
) {
    consumeDeterministically(
        first,
    )
    consumeDeterministically(
        second,
    )

    consumeDeterministically(
        <!INVALID_MOVED_ACCESS!>first<!>,
    )
    consumeDeterministically(
        <!INVALID_MOVED_ACCESS!>second<!>,
    )
}

// Boundary control: two diagnostics share one source range. Their messages
// must retain the collector's deterministic lexical tie-break order.
fun `same location diagnostics have stable wording order`(box: @Borrowed @Unique DeterminismBox) {
    val alias: @Borrowed @Unique DeterminismBox = box
    consumeDeterministically(<!INVALID_MOVED_ACCESS, LOCALITY_MISMATCH!>box<!>)
}
