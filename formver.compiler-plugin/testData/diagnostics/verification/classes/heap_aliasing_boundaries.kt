import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.postconditions

class BoundaryCell(@property:Unique var value: Int)

class NullableBoundaryCell(@property:Unique var cell: BoundaryCell?)

@Unique
fun <!VIPER_TEXT!>constructBelowBoundary<!>(): BoundaryCell {
    postconditions<BoundaryCell> { result -> result.value == -1 }
    return BoundaryCell(-1)
}

@Unique
fun <!VIPER_TEXT!>constructAtBoundary<!>(): BoundaryCell {
    postconditions<BoundaryCell> { result -> result.value == 0 }
    return BoundaryCell(0)
}

@Unique
fun <!VIPER_TEXT!>constructAboveBoundary<!>(): BoundaryCell {
    postconditions<BoundaryCell> { result -> result.value == 1 }
    return BoundaryCell(1)
}

@Unique
fun constructMinimum(): BoundaryCell = BoundaryCell(<!INTERNAL_ERROR!>Int<!>.MIN_VALUE)

@Unique
fun constructMaximum(): BoundaryCell = BoundaryCell(<!INTERNAL_ERROR!>Int<!>.MAX_VALUE)

<!VIPER_VERIFICATION_ERROR!>fun <!VIPER_TEXT!>mutateThroughUniqueAlias<!>(@Unique cell: BoundaryCell): Int {
    postconditions<Int> { result -> result == 1 }
    val alias: @Unique BoundaryCell = cell
    alias.value = 1
    return alias.value
}<!>

<!VIPER_VERIFICATION_ERROR!>fun <!VIPER_TEXT!>mutateThroughSharedAlias<!>(cell: BoundaryCell): Int {
    postconditions<Int> { result -> result == 1 }
    val alias = cell
    alias.value = 1
    return cell.value
}<!>

@Unique
fun <!VIPER_TEXT!>nullableEmpty<!>(): BoundaryCell? {
    postconditions<BoundaryCell?> { result -> result == null }
    return null
}

@Unique
fun <!VIPER_TEXT!>nullableSingleton<!>(): BoundaryCell? {
    postconditions<BoundaryCell?> { result -> result != null }
    return BoundaryCell(0)
}

<!VIPER_VERIFICATION_ERROR!>fun <!VIPER_TEXT!>replaceNullableEmpty<!>(@Unique @Borrowed holder: NullableBoundaryCell) {
    postconditions<Unit> { holder.cell == null }
    holder.cell = null
}<!>

<!VIPER_VERIFICATION_ERROR!>fun <!VIPER_TEXT!>replaceNullableSingleton<!>(@Unique @Borrowed holder: NullableBoundaryCell) {
    postconditions<Unit> { holder.cell != null }
    holder.cell = BoundaryCell(0)
}<!>
