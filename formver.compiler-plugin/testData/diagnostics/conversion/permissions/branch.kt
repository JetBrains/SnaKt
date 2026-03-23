import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

class A(
    @Unique var um: B,  // unique-mutable
    @Unique val ui: B,  // unique-immutable
    var sm: B,          // shared-mutable
    val si: B,          // shared-immutable
)


class B(
    @Unique var um: C,  // unique-mutable
    @Unique val ui: C,  // unique-immutable
    var sm: C,          // shared-mutable
    val si: C,          // shared-immutable
)


class C(
    @Unique var um: Any,  // unique-mutable
    @Unique val ui: Any,  // unique-immutable
    var sm: Any,          // shared-mutable
    val si: Any,          // shared-immutable
)

class D(
    @Unique var um: A?
)

fun <!VIPER_TEXT!>simpleBranch<!>(@Unique a : A) : Int {
    if (a.ui == a.si) {
        return 1
    } else {
        return 2
    }
}

fun <!VIPER_TEXT!>simpleBranchWithoutElse<!>(@Unique a : A) : Int {
    if (a.ui == a.si) {
        return 1
    }
    return 2
}

fun <!VIPER_TEXT!>simpleBranchWithOutsideCondition<!>(@Unique a : A) : Int {
    val b = (a.ui == a.si)
    if (b) {
        return 1
    } else {
        return 2
    }
}

fun <!VIPER_TEXT!>movedOverBranch<!>(@Unique a: A) {
    @Unique val x = a.um

    if (a.ui == a.si) {
        a.um = x
    } else {
        a.um = x
    }
}

fun <!VIPER_TEXT!>movedOverBranchWithoutMovingBack<!>(@Unique a: A)  {
    @Unique val x = a.um

    if (a.ui == a.si) {
        a.um = x
    }
}

fun <!VIPER_TEXT!>deepCondition<!>(@Unique a : A) : Int {

    if (a.um == a.si && a.um.um == a.si.um) {
        return 2
    }
    return 1
}


fun <!VIPER_TEXT!>nullTest<!>(@Unique d : D) : Int {

    if (d.um == null) {
        return 1
    } else {
        return 2
    }


}