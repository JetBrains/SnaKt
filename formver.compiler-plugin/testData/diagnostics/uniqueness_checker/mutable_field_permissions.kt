// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class MutableFieldValue

class MutableFieldPair(
    var first: @Unique MutableFieldValue,
    var second: @Unique MutableFieldValue,
)

fun borrowField(value: @Borrowed MutableFieldValue) {}

fun consumeField(value: @Unique MutableFieldValue) {}

fun consumePair(value: @Unique MutableFieldPair) {}

fun chooseBranch(): Boolean = false

fun `shared reference can read both mutable fields`(pair: MutableFieldPair) {
    borrowField(pair.first)
    borrowField(pair.second)
}

fun `shared reference cannot transfer a mutable field`(pair: MutableFieldPair) {
    consumeField(<!UNIQUENESS_MISMATCH!>pair.first<!>)
    borrowField(pair.second)
}

fun `shared reference can replace and read a mutable field`(
    pair: MutableFieldPair,
    replacement: @Unique MutableFieldValue,
) {
    pair.first = replacement
    borrowField(pair.first)
    borrowField(pair.second)
}

fun `unique reference can restore first field after call`(
    pair: @Unique MutableFieldPair,
    replacement: @Unique MutableFieldValue,
) {
    consumeField(pair.first)
    borrowField(pair.second)
    pair.first = replacement
    consumePair(pair)
}

fun `moving first field does not transfer second field permission`(
    pair: @Unique MutableFieldPair,
) {
    consumeField(pair.first)
    borrowField(<!INVALID_MOVED_ACCESS!>pair.first<!>)
    borrowField(pair.second)
    consumeField(pair.second)
}

fun `restoring the moved field only in one branch loses pair permission`(
    pair: @Unique MutableFieldPair,
    replacement: @Unique MutableFieldValue,
) {
    consumeField(pair.first)
    if (chooseBranch()) {
        pair.first = replacement
    }
    consumePair(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>pair<!>)
}

fun `restoring the selected field in each branch preserves pair permission`(
    pair: @Unique MutableFieldPair,
    firstReplacement: @Unique MutableFieldValue,
    secondReplacement: @Unique MutableFieldValue,
) {
    if (chooseBranch()) {
        consumeField(pair.first)
        pair.first = firstReplacement
    } else {
        consumeField(pair.second)
        pair.second = secondReplacement
    }
    consumePair(pair)
}

fun `shared value cannot be written into unique mutable field`(
    pair: @Unique MutableFieldPair,
    shared: MutableFieldValue,
) {
    pair.first = <!UNIQUENESS_MISMATCH!>shared<!>
    borrowField(pair.second)
}
