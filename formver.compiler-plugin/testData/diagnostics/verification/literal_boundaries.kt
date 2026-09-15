// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

private const val FOLDED_INT = 0x20 + 0b1010

@AlwaysVerify
fun <!VIPER_TEXT!>integerBasesAtBoundaries<!>() {
    val zero = 0x0
    val positiveMax = 0x7fff_ffff
    val negativeMin = -0x8000_0000
    val binary = 0b1010_1010

    verify(
        zero == 0,
        positiveMax == 2_147_483_647,
        negativeMin == -2_147_483_648,
        binary == 170,
    )
}

@AlwaysVerify
fun <!VIPER_TEXT!>escapedCharactersAtBoundaries<!>() {
    val nul = '\u0000'
    val newline = '\n'
    val max = '\uFFFF'

    verify(
        nul == '\u0000',
        newline == '\u000A',
        max == '\uFFFF',
    )
}

@AlwaysVerify
fun <!VIPER_TEXT!>literalStringTemplates<!>() {
    val decimal = "value=${42}"
    val escaped = "${'\n'}:${'\uFFFF'}"

    verify(
        decimal == "value=42",
        escaped == "\n:\uFFFF",
    )
}

<!INTERNAL_ERROR!>@AlwaysVerify
fun foldedConstants() {
    val locallyFoldable = 0x20 + 0b1010
    verify(
        locallyFoldable == 42,
        FOLDED_INT == 42,
    )
}<!>

@AlwaysVerify
fun unsignedSuffixBoundaries() {
    val uintMax = <!INTERNAL_ERROR!>0xffff_ffffu<!>

    verify(
        uintMax == 4_294_967_295u,
    )
}
