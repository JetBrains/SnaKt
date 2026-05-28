// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

<!ADT_VIOLATION, ADT_VIOLATION!>@ADT
sealed interface Direction<!>

@ADT
data <!ADT_VIOLATION, ADT_VIOLATION!>object North<!> : Direction

@ADT
data <!ADT_VIOLATION, ADT_VIOLATION!>object South<!> : Direction

@ADT
data <!ADT_VIOLATION, ADT_VIOLATION!>object East<!> : Direction

@ADT
data <!ADT_VIOLATION, ADT_VIOLATION!>object West<!> : Direction

<!ADT_VIOLATION, PURITY_VIOLATION, PURITY_VIOLATION!>@Pure
fun opposite(d: Direction): Direction = when (d) {
        is North -> <!ADT_VIOLATION, ADT_VIOLATION!>South<!>
        is South -> <!ADT_VIOLATION, ADT_VIOLATION!>North<!>
        is East -> <!ADT_VIOLATION, ADT_VIOLATION!>West<!>
        is West -> <!ADT_VIOLATION, ADT_VIOLATION!>East<!>
    }<!>

<!ADT_VIOLATION!>@AlwaysVerify
fun oppositeInvolution(d: Direction): Unit {
    postconditions<Unit> {
        opposite(opposite(d)) == d
    }
    when (d) {
        is North -> {}
        is South -> {}
        is East -> {}
        is West -> {}
    }
}<!>
