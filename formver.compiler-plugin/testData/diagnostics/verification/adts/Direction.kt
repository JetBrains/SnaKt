// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

@ADT
sealed interface Direction

@ADT
data object North : Direction

@ADT
data object South : Direction

@ADT
data object East : Direction

@ADT
data object West : Direction

@Pure
fun <!VIPER_TEXT!>opposite<!>(d: Direction): Direction = when (d) {
        is North -> South
        is South -> North
        is East -> West
        is West -> East
    }

@AlwaysVerify
fun <!VIPER_TEXT!>oppositeInvolution<!>(d: Direction): Unit {
    postconditions<Unit> {
        opposite(opposite(d)) == d
    }
    when (d) {
        is North -> {}
        is South -> {}
        is East -> {}
        is West -> {}
    }
}
