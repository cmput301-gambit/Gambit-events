package com.campsitereservationapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [Reservation].
 *
 * **Purpose:** checks validation, cancelling, and the clash check that
 * stops two reservations booking the same campsite on the same days
 * (US 01.05.01).
 *
 * **Outstanding issues:** None.
 */
class ReservationTest {

    private fun december(start: Int, finish: Int) =
        DateRange(LocalDate.of(2026, 12, start), LocalDate.of(2026, 12, finish))

    private val priyaAtPineRidge = Reservation(
        id = "reservation-1",
        entrantId = "entrant-priya",
        campsiteId = "campsite-pine-ridge",
        lotteryId = "lottery-pine-ridge",
        dates = december(10, 12),
    )

    @Test
    fun newReservation_isConfirmedAndValid() {
        assertTrue(priyaAtPineRidge.isActive)
        assertTrue(priyaAtPineRidge.isValid())
    }

    @Test
    fun blankIds_areInvalid() {
        val empty = Reservation(dates = december(10, 12))

        assertEquals(
            listOf(ReservationError.BLANK_ENTRANT, ReservationError.BLANK_CAMPSITE, ReservationError.BLANK_LOTTERY),
            empty.validate(),
        )
    }

    @Test
    fun cancelled_returnsCancelledCopyAndLeavesOriginal() {
        val cancelled = priyaAtPineRidge.cancelled()

        assertEquals(ReservationStatus.CANCELLED, cancelled?.status)
        assertTrue(priyaAtPineRidge.isActive)
    }

    @Test
    fun cancellingTwice_returnsNull() {
        assertNull(priyaAtPineRidge.cancelled()!!.cancelled())
    }

    @Test
    fun rejecting_marksRejectedAndFreesTheDates() {
        val rejected = priyaAtPineRidge.rejected("Forest fire")

        assertEquals(ReservationStatus.REJECTED, rejected?.status)
        assertFalse(rejected!!.isActive)
        assertFalse(rejected.clashesWith(priyaAtPineRidge.copy(id = "reservation-2")))
    }

    @Test
    fun rejecting_keepsTheTrimmedReason() {
        assertEquals("Forest fire", priyaAtPineRidge.rejected("  Forest fire  ")?.rejectionReason)
    }

    @Test
    fun rejecting_withBlankOrTooLongReason_returnsNull() {
        assertNull(priyaAtPineRidge.rejected("   "))
        assertNull(priyaAtPineRidge.rejected("x".repeat(Reservation.MAX_REASON_LENGTH + 1)))
    }

    @Test
    fun rejectingACancelledOrRejectedReservation_returnsNull() {
        assertNull(priyaAtPineRidge.cancelled()!!.rejected("Forest fire"))
        assertNull(priyaAtPineRidge.rejected("Forest fire")!!.rejected("Forest fire"))
    }

    @Test
    fun rejecting_aStayThatAlreadyEnded_returnsNull() {
        val dayAfter = LocalDate.of(2026, 12, 13)

        assertFalse(priyaAtPineRidge.canBeRejected(dayAfter))
        assertNull(priyaAtPineRidge.rejected("Forest fire", today = dayAfter))
    }

    @Test
    fun rejecting_onTheLastDayOfTheStay_isAllowed() {
        val lastDay = LocalDate.of(2026, 12, 12)

        assertTrue(priyaAtPineRidge.canBeRejected(lastDay))
        assertEquals(ReservationStatus.REJECTED, priyaAtPineRidge.rejected("Forest fire", today = lastDay)?.status)
    }

    @Test
    fun overlappingDatesAtSameCampsite_clash() {
        val ali = priyaAtPineRidge.copy(id = "reservation-2", entrantId = "entrant-ali", dates = december(12, 14))

        assertTrue(priyaAtPineRidge.clashesWith(ali))
    }

    @Test
    fun separateDatesAtSameCampsite_dontClash() {
        val ali = priyaAtPineRidge.copy(id = "reservation-2", entrantId = "entrant-ali", dates = december(13, 15))

        assertFalse(priyaAtPineRidge.clashesWith(ali))
    }

    @Test
    fun sameDatesAtDifferentCampsite_dontClash() {
        val ali = priyaAtPineRidge.copy(id = "reservation-2", entrantId = "entrant-ali", campsiteId = "campsite-aspen-hollow")

        assertFalse(priyaAtPineRidge.clashesWith(ali))
    }

    @Test
    fun cancelledReservation_doesntClash() {
        val ali = priyaAtPineRidge.copy(id = "reservation-2", entrantId = "entrant-ali")

        assertFalse(priyaAtPineRidge.cancelled()!!.clashesWith(ali))
    }
}