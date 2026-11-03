package com.campsitereservationapp.model

import java.time.LocalDate

/**
 * Represents a range of dates.
 *
 * **Purpose:** An easy way to interact with a range of dates, which many sections
 * of the project require.
 *
 * **Design rationale:** N/A
 *
 * **Outstanding issues:**
 */

data class DateRange(
    val start: LocalDate,
    val finish: LocalDate,
) {
    init {
        assert(!finish.isBefore(start))
        assert(!start.isAfter(finish))
    }


    /**
     * Returns whether a given date is within the date range.
     *
     * @return a [Boolean] of whether the date is valid and within the range.
     */
    fun isWithin(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(finish)
}