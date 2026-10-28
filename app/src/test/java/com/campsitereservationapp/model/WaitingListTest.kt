package com.campsitereservationapp.model

import org.junit.Assert.assertTrue
import org.junit.Test

class WaitingListTest {

    val entrantId = "testUser1"
    val entrant = Entrant(entrantId)

    @Test
    fun validWaitingListHasNoErrors() {
        val waitingList = WaitingList()
        assertTrue(waitingList.isValid())
    }

    @Test
    fun addEntrantFull() {
        val waitingList = WaitingList(entrantCap = 3)
        val entrant1 = Entrant("testEntrant1")
        val entrant2 = Entrant("testEntrant2")
        val entrant3 = Entrant("testEntrant3")
        val entrant4 = Entrant("testEntrant4")
        waitingList.addEntrant(entrant1)
        waitingList.addEntrant(entrant2)
        waitingList.addEntrant(entrant3)
        assert(waitingList.entrantList.size == 3) // All three entrants successfully added
        assert(waitingList.isFull)
        assert(!waitingList.addEntrant(entrant4))

    }

    @Test
    fun removeEntrantFull() {
        val entrant1 = Entrant("testEntrant1")
        val entrant2 = Entrant("testEntrant2")
        val waitingList = WaitingList(entrantList = mutableListOf(entrant1.userId, entrant2.userId),
            entrantCap = 2, isFull = true)
        waitingList.removeEntrant(entrant1)
        assert(!waitingList.entrantList.contains(entrant1.userId))
        assert(!waitingList.isFull)
        waitingList.removeEntrant(entrant2)
        assert(!waitingList.entrantList.contains(entrant2.userId))
        assert(!waitingList.isFull)
    }




}