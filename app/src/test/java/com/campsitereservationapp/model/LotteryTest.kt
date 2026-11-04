package com.campsitereservationapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

/**
 * Unit tests for [Lottery].
 *
 * **Purpose:** checks validation, joining and leaving, drawing winners, and
 * marking notifications as sent.
 *
 * **Design rationale:** Lottery is immutable, so each test checks the copy
 * a method returns (or null when the change isn't allowed). Draw tests pass
 * a seeded Random so results are repeatable.
 *
 * **Outstanding issues:** None.
 */
class LotteryTest {
    val testCampsite = Campsite("camp1", ownerId = "Potato-Boy")
    val registrationPeriod = DateRange(
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5))

    val reservationPeriod = DateRange(
        LocalDate.now().plusDays(6),
        LocalDate.now().plusDays(8))

    val entrantId = "testUser1"

    private fun openLottery(waitingList: WaitingList = WaitingList(), winnerAmount: Int = 1) =
        Lottery("lottery-moose", ownerId = testCampsite.ownerId,
            campsiteIds = listOf(testCampsite.id), registrationPeriod = registrationPeriod,
            reservationPeriod = reservationPeriod, stage = LotteryStatus.OPEN,
            winnerAmount = winnerAmount, waitingList = waitingList)

    @Test
    fun validLotteryHasNoErrors() {
        val lottery = Lottery("lottery1", ownerId = testCampsite.ownerId,
            campsiteIds = listOf(testCampsite.id), registrationPeriod = registrationPeriod,
            reservationPeriod = reservationPeriod)
        assertTrue(lottery.isValid())
    }

    @Test
    fun addEntrantOpen() {
        val joined = openLottery().withEntrant(entrantId)

        assertTrue(entrantId in joined!!.waitingList.entrantList)
    }

    @Test
    fun addEntrantNotOpen() {
        val draft = openLottery().copy(stage = LotteryStatus.DRAFT)

        assertNull(draft.withEntrant(entrantId))
    }

    @Test
    fun removeEntrantOpen() {
        val lottery = openLottery(WaitingList(entrantList = listOf(entrantId)))

        val left = lottery.withoutEntrant(entrantId)

        assertFalse(entrantId in left!!.waitingList.entrantList)
    }

    @Test
    fun removeEntrantNotOpen() {
        val closed = openLottery(WaitingList(entrantList = listOf(entrantId)))
            .copy(stage = LotteryStatus.CLOSED)

        assertNull(closed.withoutEntrant(entrantId))
    }

    @Test
    fun drawn_picksExactlyWinnerAmountDistinctWinners() {
        val entrants = listOf("entrant-priya", "entrant-noah", "entrant-ali", "entrant-ben", "entrant-sara")
        val lottery = openLottery(WaitingList(entrantList = entrants), winnerAmount = 2)
            .copy(stage = LotteryStatus.DRAWN)

        val drawn = lottery.drawn(Random(42))!!

        assertEquals(LotteryStatus.CLOSED, drawn.stage)
        assertEquals(2, drawn.waitingList.entrantWinners.toSet().size)
        assertEquals(3, drawn.waitingList.entrantLosers.size)
        assertEquals(entrants.toSet(), (drawn.waitingList.entrantWinners + drawn.waitingList.entrantLosers).toSet())
    }

    @Test
    fun drawn_withMoreSpotsThanEntrants_everyoneWins() {
        val lottery = openLottery(WaitingList(entrantList = listOf("entrant-priya", "entrant-noah")), winnerAmount = 5)
            .copy(stage = LotteryStatus.DRAWN)

        val drawn = lottery.drawn()!!

        assertEquals(2, drawn.waitingList.entrantWinners.size)
        assertTrue(drawn.waitingList.entrantLosers.isEmpty())
    }

    @Test
    fun drawn_whenNotReadyToDraw_returnsNull() {
        assertNull(openLottery().drawn())
    }

    @Test
    fun markNotified_onlyOnce() {
        val closed = openLottery().copy(stage = LotteryStatus.CLOSED)

        val notified = closed.markNotified()!!

        assertTrue(notified.sentNotifications)
        assertNull(notified.markNotified())
    }

    @Test
    fun readyToDraw_dayAfterRegistrationEnds_movesToDrawn() {
        val dayAfter = registrationPeriod.finish.plusDays(1)

        val ready = openLottery().readyToDraw(dayAfter)

        assertEquals(LotteryStatus.DRAWN, ready?.stage)
    }

    @Test
    fun readyToDraw_onLastDayOfRegistration_returnsNull() {
        assertNull(openLottery().readyToDraw(registrationPeriod.finish))
    }

    @Test
    fun readyToDraw_whenNotOpen_returnsNull() {
        val dayAfter = registrationPeriod.finish.plusDays(1)
        val draft = openLottery().copy(stage = LotteryStatus.DRAFT)

        assertNull(draft.readyToDraw(dayAfter))
    }
}