package com.campsitereservationapp.model

import com.campsitereservationapp.data.DateRange
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LotteryTest {
    val testCampsite = Campsite("camp1",)
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
        val lottery = Lottery("lottery1", mutableListOf(testCampsite.id),
            registrationPeriod, reservationPeriod)
        assertTrue(lottery.isValid())
    }

    @Test
    fun addEntrantOpen() {
        val lottery = Lottery("lottery2", mutableListOf(testCampsite.id),
            registrationPeriod, reservationPeriod)
        lottery.open()
        lottery.addEntrant(entrant)
        assertTrue(lottery.waitingList.entrantList.contains(entrant.userId))
    }

    @Test
    fun addEntrantNotOpen() {
        val lottery = Lottery("lottery3", mutableListOf(testCampsite.id),
            registrationPeriod, reservationPeriod)
        lottery.addEntrant(entrant)
        assertTrue(!lottery.waitingList.entrantList.contains(entrant.userId))

    }

    @Test
    fun removeEntrantOpen() {
        val lottery = Lottery("lottery4", mutableListOf(testCampsite.id),
            registrationPeriod, reservationPeriod, waitingList = WaitingList(mutableListOf<String>(entrantId)))
        lottery.open()
        lottery.removeEntrant(entrant)
        assertTrue(!lottery.waitingList.entrantList.contains(entrant.userId))

    }

    @Test
    fun removeEntrantNotOpen() {
        val lottery = Lottery("lottery5", mutableListOf(testCampsite.id),
            registrationPeriod, reservationPeriod, waitingList = WaitingList(mutableListOf<String>(entrantId)))
        lottery.removeEntrant(entrant)
        assertTrue(lottery.waitingList.entrantList.contains(entrant.userId))

    }


}