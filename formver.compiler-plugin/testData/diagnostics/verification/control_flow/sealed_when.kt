// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

sealed interface Packet

class Payload(val value: Int) : Packet

class Empty : Packet

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>exhaustivePayloadField<!>(packet: Packet): Int =
    when (packet) {
        is Payload -> packet.value
        is Empty -> 0
    }<!>

@AlwaysVerify
fun <!VIPER_TEXT!>sealedCasesAreExhaustive<!>(packet: Packet) {
    verify(<!VIPER_VERIFICATION_ERROR!>packet is Payload || packet is Empty<!>)
}

interface OpenPacket

class OpenPayload(val value: Int) : OpenPacket

class OpenEmpty : OpenPacket

@AlwaysVerify
fun <!VIPER_TEXT!>openHierarchyNeedsFallback<!>(packet: OpenPacket): Int =
    when (packet) {
        is OpenPayload -> packet.value
        is OpenEmpty -> 0
        else -> -1
    }
