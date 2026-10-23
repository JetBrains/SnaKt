// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify


class CustomList(override val size: Int, value: Int) : AbstractList<Int>() {
    override fun <!VIPER_TEXT!>get<!>(index: Int): Int = value
    private val value = value
}

@AlwaysVerify
fun <!VIPER_TEXT!>test<!>(n: Int) {
    val customList = CustomList(n, 0)
    if (!customList.isEmpty()) {
        customList[customList.size - 1]
        customList[0]
    }
}

class Grid(override val size: Int) : AbstractList<Int>() {
    override fun <!VIPER_TEXT!>get<!>(index: Int): Int = index
    fun <!VIPER_TEXT!>get<!>(row: Int, col: Int): Int = row * col
}

fun <!VIPER_TEXT!>emptyList<!>(): List<Int> = listOf(1)

@AlwaysVerify
fun <!VIPER_TEXT!>lookalikes<!>(grid: Grid): Int {
    val notEmpty = emptyList()
    return grid.get(-1, 5)
}
