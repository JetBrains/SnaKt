// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Pure

open class Base_PureFun {
    @Pure
    open fun <!VIPER_TEXT!>payload<!>(): Int = 1
}

open class Base {
    open class PureFun {
        @Pure
        open fun <!VIPER_TEXT!>payload<!>(): Boolean = true
    }
}

class Derived_Base_PureFun : Base_PureFun()

class Derived_Base {
    class PureFun : Base.PureFun()
}

fun <!VIPER_TEXT!>pureFlattenedInheritedMemberFunctions<!>() {
    val left = Derived_Base_PureFun()
    val right = Derived_Base.PureFun()
    left.payload()
    right.payload()
}
