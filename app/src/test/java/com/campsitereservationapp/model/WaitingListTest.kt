package com.campsitereservationapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        val full = WaitingList(entrantCap = 3)
            .withEntrant("entrant-priya")!!
            .withEntrant("entrant-noah")!!
            .withEntrant("entrant-ali")!!

        assertEquals(3, full.entrantList.size)
        assertTrue(full.isFull)
        assertNull(full.withEntrant("entrant-sara"))
    }

    @Test
    fun removeEntrantFull() {
        val waitingList = WaitingList(
            entrantList = listOf("entrant-priya", "entrant-noah"), entrantCap = 2, isFull = true,
        )

        val updated = waitingList.withoutEntrant("entrant-priya")!!

        assertFalse("entrant-priya" in updated.entrantList)
        assertFalse(updated.isFull)
    }

    @Test
    fun removeEntrant_leavesTheOriginalUnchanged() {
        val waitingList = WaitingList(entrantList = listOf("entrant-priya"))

        waitingList.withoutEntrant("entrant-priya")

        assertTrue("entrant-priya" in waitingList.entrantList)
    }

    @Test
    fun entrantDeclined() {
        val waitingList = WaitingList(
            entrantList = listOf("entrant-priya"), entrantWinners = listOf("entrant-priya"),
        )

        val updated = waitingList.withDecline("entrant-priya")!!

        assertTrue("entrant-priya" in updated.entrantsDeclined)
        assertFalse("entrant-priya" in updated.entrantWinners)
    }



}