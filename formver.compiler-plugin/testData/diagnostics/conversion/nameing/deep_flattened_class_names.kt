// NEVER_VALIDATE

class Gamma_Delta_Eta {
    class Zed(val data: Int)
}

class Gamma {
    class Delta_Eta {
        class Zed(val data: Boolean)
    }
}

fun <!VIPER_TEXT!>deepFlattenedClassNames<!>() {
    val left = Gamma_Delta_Eta.Zed(0)
    val right = Gamma.Delta_Eta.Zed(true)
    left.data
    right.data
}
