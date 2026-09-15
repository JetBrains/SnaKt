// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique

class Payload

class HeapBox(
    var current: @Unique Payload,
    var spare: @Unique Payload,
    var optional: @Unique Payload?,
)

fun consume(value: @Unique Any?) {}

// Positive control: construction transfers all three arguments into the box.
fun `constructor transfer creates a consumable object`(
    first: @Unique Payload,
    second: @Unique Payload,
) {
    val box: @Unique HeapBox = HeapBox(first, second, null)

    consume(box)
    consume(<!INVALID_MOVED_ACCESS!>first<!>)
    consume(<!INVALID_MOVED_ACCESS!>second<!>)
}

// Boundary control: moving one field leaves its independent sibling usable.
fun `moved field does not move sibling`(box: @Unique HeapBox) {
    val moved: @Unique Payload = box.current

    consume(moved)
    consume(box.spare)
}

// Field extraction and root escape interact: the root retains a moved field.
fun `moved field prevents consuming root`(box: @Unique HeapBox) {
    val moved: @Unique Payload = box.current

    consume(moved)
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>box<!>)
}

// Setter interaction: replacing the extracted field restores the whole object.
fun `replacing moved field restores root`(box: @Unique HeapBox) {
    val moved: @Unique Payload = box.current
    consume(moved)

    box.current = Payload()
    consume(box)
}

// Nullable-field extraction has the same ownership effect as a non-null field.
fun `moving nullable field prevents consuming root`(box: @Unique HeapBox) {
    val moved: @Unique Payload? = box.optional

    consume(moved)
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>box<!>)
}

// Nullable setter boundary: null is a fresh replacement for the moved slot.
fun `replacing moved nullable field with null restores root`(box: @Unique HeapBox) {
    val moved: @Unique Payload? = box.optional
    consume(moved)

    box.optional = null
    consume(box)
}

// Constructor transfer, a local alias, and mutation are exercised together.
fun `mutate constructed object through unique alias`(
    first: @Unique Payload,
    second: @Unique Payload,
    replacement: @Unique Payload,
) {
    val alias: @Unique HeapBox = HeapBox(first, second, null)

    alias.current = replacement
    consume(alias)
    consume(<!INVALID_MOVED_ACCESS!>first<!>)
    consume(<!INVALID_MOVED_ACCESS!>second<!>)
    consume(<!INVALID_MOVED_ACCESS!>replacement<!>)
}
