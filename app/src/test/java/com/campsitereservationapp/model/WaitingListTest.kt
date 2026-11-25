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

    // ---- withoutUser (US 01.08.01, 03.02.01) ----

    @Test
    fun withoutUser_removesThemFromEveryList() {
        val drawn = WaitingList(
            entrantList = listOf("entrant-priya", "entrant-noah", "entrant-ali"),
            entrantWinners = listOf("entrant-noah"),
            entrantLosers = listOf("entrant-priya", "entrant-ali"),
        )

        val updated = drawn.withoutUser("entrant-priya")!!

        assertEquals(listOf("entrant-noah", "entrant-ali"), updated.entrantList)
        assertEquals(listOf("entrant-ali"), updated.entrantLosers)
        assertEquals(listOf("entrant-noah"), updated.entrantWinners)
    }

    @Test
    fun withoutUser_removesADeclinedWinner() {
        val declined = WaitingList(
            entrantList = listOf("entrant-priya"),
            entrantsDeclined = listOf("entrant-priya"),
        )

        val updated = declined.withoutUser("entrant-priya")!!

        assertTrue(updated.entrantList.isEmpty())
        assertTrue(updated.entrantsDeclined.isEmpty())
    }

    @Test
    fun withoutUser_opensUpAFullList() {
        val full = WaitingList(
            entrantList = listOf("entrant-priya", "entrant-noah"), entrantCap = 2, isFull = true,
        )

        assertFalse(full.withoutUser("entrant-priya")!!.isFull)
    }

    @Test
    fun withoutUser_notOnAnyList_returnsNull() {
        val waitingList = WaitingList(entrantList = listOf("entrant-noah"))

        assertNull(waitingList.withoutUser("entrant-priya"))
    }
}
