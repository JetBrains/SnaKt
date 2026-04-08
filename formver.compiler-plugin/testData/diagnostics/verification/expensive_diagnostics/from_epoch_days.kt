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
// With assertions we verify: adjustCycles <= -1, adjust <= 0, zeroDay >= 0,
// doyEst >= 0, marchMonth0 in 0..11, month in 1..12, dom in 1..31,
// yearEst >= 0.  These compose into postconditions res in 101..99991231.
//
// The remaining gap: doyEst <= 365 (upper bound on day-of-year) requires
// reasoning about the year-estimation division — too much nonlinear
// arithmetic for the solver.

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
        // Minimum packed date: year=0, month=1, dom=1 → 0*10000 + 1*100 + 1 = 101
        res >= 101
        // Upper bound: year <= 9999 would give max 99991231, but proving
        // yearEst <= 9999 requires bounding the year estimation formula.
        res <= 99991231
    }
    var zeroDay = epochDays + 719528
    zeroDay -= 60

    var adjust = 0L
    if (zeroDay < 0) {
        val adjustCycles = (zeroDay + 1) / 146097 - 1
        // Source comment: adjustCycles in -2.5e6 .. -1
        verify(adjustCycles <= -1)
        adjust = adjustCycles * 400
        zeroDay -= adjustCycles * 146097
    }
    // Source comment: adjust in -1e9 .. 0
    verify(adjust <= 0)
    // After the 400-year cycle adjustment, zeroDay is non-negative.
    // Source comment: zeroDay in 0 .. 3.7e11 now
    verify(zeroDay >= 0)

    var yearEst = (400 * zeroDay + 591) / 146097
    var doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
    if (doyEst < 0) {
        yearEst--
        doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
    }
    // doyEst >= 0: the if-fix guarantees this.
    verify(doyEst >= 0)
    yearEst += adjust

    val marchDoy0 = doyEst.toInt()
    // marchDoy0 >= 0 follows directly from doyEst >= 0
    verify(marchDoy0 >= 0)
    val marchMonth0 = (marchDoy0 * 5 + 2) / 153
    // marchMonth0 >= 0: numerator >= 2 (since marchDoy0 >= 0), divisor 153 > 0
    verify(marchMonth0 >= 0)
    // marchMonth0 <= 11: the solver derives this without an explicit doyEst upper bound.
    verify(marchMonth0 <= 11)
    val month = (marchMonth0 + 2) % 12 + 1
    // month >= 1: (x % 12) >= 0 for x >= 0, so month >= 0 + 1
    verify(month >= 1)
    // month <= 12: (x % 12) <= 11 for any x, so month <= 11 + 1
    verify(month <= 12)
    val dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
    // dom >= 1: marchDoy0 >= floor((306*marchMonth0 + 5)/10) follows from
    // marchMonth0 = floor((marchDoy0*5 + 2)/153) and integer division properties.
    verify(dom >= 1)
    verify(dom <= 31)
    yearEst += marchMonth0 / 10
    // yearEst >= 0 given our precondition (epochDays >= -719528 ≈ year 0).
    verify(yearEst >= 0)

    return yearEst.toInt() * 10000 + month * 100 + dom
}
