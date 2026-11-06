package com.campsitereservationapp.data

import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.model.ReservationStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [ReservationRepository].
 *
 * **Purpose:** checks that free dates can be booked, taken dates are
 * refused, cancelling frees dates, and each entrant sees only their own
 * reservations (US 01.04.01, 01.05.01).
 *
 * **Design rationale:** a fresh repository backed by a [MockDataSource] for
 * each test, so nothing touches Firestore.
 *
 * **Outstanding issues:** None.
 */
class ReservationRepositoryTest {

    private lateinit var repository: ReservationRepository

    private fun december(start: Int, finish: Int) =
        DateRange(LocalDate.of(2026, 12, start), LocalDate.of(2026, 12, finish))

    private fun reservation(
        id: String,
        entrantId: String,
        dates: DateRange,
        campsiteId: String = "campsite-pine-ridge",
    ) = Reservation(
        id = id,
        entrantId = entrantId,
        campsiteId = campsiteId,
        lotteryId = "lottery-pine-ridge",
        dates = dates,
    )

    @Before
    fun setUp() {
        repository = ReservationRepository(MockDataSource())
    }

    @Test
    fun reserve_freeDates_saves() = runTest {
        val saved = repository.reserve(reservation("r1", "entrant-priya", december(10, 12)))

        assertNotNull(saved)
        assertNotNull(repository.get("r1"))
    }

    @Test
    fun reserve_takenDates_isRefused() = runTest {
        repository.reserve(reservation("r1", "entrant-priya", december(10, 12)))

        val second = repository.reserve(reservation("r2", "entrant-ali", december(11, 13)))

        assertNull(second)
        assertNull(repository.get("r2"))
    }

    @Test
    fun reserve_afterCancelling_datesAreFreeAgain() = runTest {
        repository.reserve(reservation("r1", "entrant-priya", december(10, 12)))
        repository.cancel("r1")

        val second = repository.reserve(reservation("r2", "entrant-ali", december(11, 13)))

        assertNotNull(second)
    }

    @Test
    fun cancel_marksItCancelled() = runTest {
        repository.reserve(reservation("r1", "entrant-priya", december(10, 12)))

        repository.cancel("r1")

        assertEquals(ReservationStatus.CANCELLED, repository.get("r1")?.status)
    }

    @Test
    fun cancel_missingReservation_returnsNull() = runTest {
        assertNull(repository.cancel("does-not-exist"))
    }

    @Test
    fun observeForEntrant_showsOnlyTheirs_earliestFirst() = runTest {
        repository.reserve(reservation("r1", "entrant-priya", december(20, 21)))
        repository.reserve(reservation("r2", "entrant-priya", december(10, 11), campsiteId = "campsite-aspen-hollow"))
        repository.reserve(reservation("r3", "entrant-ali", december(5, 6), campsiteId = "campsite-lakeside-loop"))

        val shown = repository.observeForEntrant("entrant-priya").first().map { it.id }

        assertEquals(listOf("r2", "r1"), shown)
    }

    @Test
    fun bookedDates_onlyActiveOnesAtThatCampsite() = runTest {
        repository.reserve(reservation("r1", "entrant-priya", december(10, 12)))
        repository.reserve(reservation("r2", "entrant-ali", december(20, 21)))
        repository.cancel("r2")
        repository.reserve(reservation("r3", "entrant-ben", december(10, 12), campsiteId = "campsite-aspen-hollow"))

        assertEquals(listOf(december(10, 12)), repository.bookedDates("campsite-pine-ridge"))
    }
}