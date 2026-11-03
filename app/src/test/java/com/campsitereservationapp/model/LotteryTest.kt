package com.campsitereservationapp.model

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LotteryTest {
    val testCampsite = Campsite("camp1", ownerId = "Potato-Boy")
    val registrationPeriod = DateRange(
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5))

    val reservationPeriod = DateRange(
        LocalDate.now().plusDays(6),
        LocalDate.now().plusDays(8))

    val entrantId = "testUser1"
    val entrant = Entrant(entrantId)


    @Test
    fun validLotteryHasNoErrors() {
        val lottery = Lottery("lottery1", ownerId = testCampsite.ownerId,
            campsiteIds = mutableListOf(testCampsite.id), registrationPeriod =  registrationPeriod,
            reservationPeriod = reservationPeriod)
        assertTrue(lottery.isValid())
    }

    @Test
    fun addEntrantOpen() {
        val lottery = Lottery("lottery2", ownerId = testCampsite.ownerId,
            campsiteIds = mutableListOf(testCampsite.id), registrationPeriod =  registrationPeriod,
            reservationPeriod = reservationPeriod)
        lottery.open()
        lottery.addEntrant(entrant.userId)
        assertTrue(lottery.waitingList.entrantList.contains(entrant.userId))
    }

    @Test
    fun addEntrantNotOpen() {
        val lottery = Lottery("lottery3", ownerId = testCampsite.ownerId,
            campsiteIds = mutableListOf(testCampsite.id), registrationPeriod =  registrationPeriod,
            reservationPeriod = reservationPeriod)
        lottery.addEntrant(entrant.userId)
        assertTrue(!lottery.waitingList.entrantList.contains(entrant.userId))

    }

    @Test
    fun removeEntrantOpen() {
        val lottery = Lottery("lottery4", ownerId = testCampsite.ownerId,
            campsiteIds = mutableListOf(testCampsite.id), registrationPeriod =  registrationPeriod,
            reservationPeriod = reservationPeriod)
        lottery.open()
        lottery.removeEntrant(entrant.userId)
        assertTrue(!lottery.waitingList.entrantList.contains(entrant.userId))

    }

    @Test
    fun removeEntrantNotOpen() {
        val lottery = Lottery("lottery5", ownerId = testCampsite.ownerId,
            campsiteIds = mutableListOf(testCampsite.id), registrationPeriod =  registrationPeriod,
            reservationPeriod = reservationPeriod, waitingList=WaitingList(mutableListOf(entrant.userId)))
        assert(!lottery.removeEntrant(entrantId))
        assertTrue(lottery.waitingList.entrantList.contains(entrant.userId))

    }
}