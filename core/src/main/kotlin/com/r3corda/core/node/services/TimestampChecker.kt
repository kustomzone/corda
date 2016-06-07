package com.r3corda.core.node.services

import com.r3corda.core.contracts.TimestampCommand
import com.r3corda.core.seconds
import com.r3corda.core.until
import java.time.Clock
import java.time.Duration

/**
 * Checks if the given timestamp falls within the allowed tolerance interval
 */
class TimestampChecker(val clock: Clock = Clock.systemUTC(),
                       val tolerance: Duration = 30.seconds) {
    fun isValid(timestampCommand: TimestampCommand): Boolean {
        val before = timestampCommand.before
        val after = timestampCommand.after

        val now = clock.instant()

        // We don't need to test for (before == null && after == null) or backwards bounds because the TimestampCommand
        // constructor already checks that.
        if (before != null && before until now > tolerance) return false
        if (after != null && now until after > tolerance) return false
        return true
    }
}