import org.jetbrains.kotlin.formver.plugin.*

// Adaptation of kotlinx-datetime's LocalDate.fromEpochDays and toEpochDays.
// Based on ThreeTenBp (org.threeten.bp.LocalDate).
//
// Constants:
//   DAYS_0000_TO_1970 = 719528
//   DAYS_PER_CYCLE = 146097 (days in 400 Gregorian years)
//
// fromEpochDays returns a packed date: year * 10000 + month * 100 + day.
//
// Intermediate assertions guide the SMT solver through the proof.  Without
// them the solver cannot close the chain of nonlinear arithmetic from the
// input epoch day to the packed (year, month, day) result.  Each verify()
// cuts the proof into smaller pieces that the solver can handle individually.
//
// Even with assertions, stronger postconditions (month in 1..12, day in 1..31)
// remain out of reach: the solver cannot derive that
// (year*10000 + month*100 + dom) % 100 == dom, which requires nonlinear
// modular arithmetic across multiplication and addition.

@AlwaysVerify
fun <!VIPER_TEXT!>toEpochDays<!>(year: Int, month: Int, day: Int): Long {
    preconditions {
        0 <= year && year <= 9999
        1 <= month && month <= 12
        1 <= day && day <= 31
    }
    postconditions<Long> { res ->
        res >= -719528
    }
    var total = 365L * year
    total += (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400
    total += (367 * month - 362) / 12
    total += day - 1
    if (month > 2) {
        total--
        if (year % 4 != 0 || (year % 100 == 0 && year % 400 != 0)) {
            total--
        }
    }
    return total - 719528
}

@AlwaysVerify
fun <!VIPER_TEXT!>fromEpochDays<!>(epochDays: Long): Int {
    preconditions {
        epochDays >= -719528
        epochDays <= 2932896
    }
    postconditions<Int> { res ->
        res >= 0
    }
    var zeroDay = epochDays + 719528
    zeroDay -= 60

    var adjust = 0L
    if (zeroDay < 0) {
        val adjustCycles = (zeroDay + 1) / 146097 - 1
        adjust = adjustCycles * 400
        zeroDay -= adjustCycles * 146097
    }
    // After the 400-year cycle adjustment, zeroDay is non-negative.
    // The solver needs this pinned because the adjustment logic involves
    // division by 146097 which is opaque to nonlinear reasoning.
    verify(zeroDay >= 0)

    var yearEst = (400 * zeroDay + 591) / 146097
    var doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
    if (doyEst < 0) {
        yearEst--
        doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
    }
    // The if-fix guarantees doyEst >= 0, but the solver can't derive the
    // upper bound (< 366) from the year-estimate division without help.
    // Bounding doyEst would let the solver bound marchMonth0 (0..11),
    // month (1..12), and dom (1..31), but that chain of nonlinear reasoning
    // involving divisors 153, 306, and 10 remains too expensive.
    verify(doyEst >= 0)
    yearEst += adjust

    val marchDoy0 = doyEst.toInt()
    val marchMonth0 = (marchDoy0 * 5 + 2) / 153
    val month = (marchMonth0 + 2) % 12 + 1
    val dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
    yearEst += marchMonth0 / 10

    return yearEst.toInt() * 10000 + month * 100 + dom
}
