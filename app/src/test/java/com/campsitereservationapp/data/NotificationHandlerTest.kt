package com.campsitereservationapp.data

import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.NotificationType
import com.campsitereservationapp.model.WaitingList
import com.campsitereservationapp.service.NotificationHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [com.campsitereservationapp.service.NotificationHandler].
 *
 * **Purpose:** checks that a drawn lottery creates one win or lose
 * notification per entrant (US 01.03.01, 01.03.02).
 */
class NotificationHandlerTest {
    private val handler = NotificationHandler()
    private val createdOn = LocalDate.of(2026, 11, 2)
    private val pineRidge = Campsite("campsite-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge")
    private val registrationPeriod = DateRange(
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(5)
    )
    private val reservationPeriod = DateRange(
        LocalDate.now().plusDays(6),
        LocalDate.now().plusDays(8)
    )

    private fun closedLottery(winners: List<String>, losers: List<String>) = Lottery(
        "lottery-pine-ridge",
        ownerId = "owner-bobby",
        campsiteIds = mutableListOf(pineRidge.id),
        registrationPeriod = registrationPeriod,
        reservationPeriod = reservationPeriod,
        stage = LotteryStatus.CLOSED,
        waitingList = WaitingList(
            entrantList = losers.toMutableList(),
            entrantWinners = winners.toMutableList(),
        ),
    )

    @Test
    fun closedLottery_winnersGetWonAndOthersGetLost() {
        val lottery = closedLottery(
            winners = listOf("entrant-priya", "entrant-noah"),
            losers = listOf("entrant-ali", "entrant-ben", "entrant-sara"),
        )

        val result = handler.createResultNotifications(lottery, listOf(pineRidge), createdOn)

        val wonIds = result.filter { it.type == NotificationType.LOTTERY_WON }.map { it.recipientId }.toSet()
        val lostIds = result.filter { it.type == NotificationType.LOTTERY_LOST }.map { it.recipientId }.toSet()
        assertEquals(5, result.size)
        assertEquals(setOf("entrant-priya", "entrant-noah"), wonIds)
        assertEquals(setOf("entrant-ali", "entrant-ben", "entrant-sara"), lostIds)
    }

    @Test
    fun realDraw_givesWinnerAmountWins() {
        val lottery = Lottery("lottery-draw", ownerId = "owner-bobby",
            campsiteIds = mutableListOf(pineRidge.id), registrationPeriod = registrationPeriod,
            reservationPeriod = reservationPeriod, winnerAmount = 2)
        lottery.open()
        listOf("entrant-priya", "entrant-noah", "entrant-ali", "entrant-ben", "entrant-sara")
            .forEach { lottery.addEntrant(it) }
        lottery.stage = LotteryStatus.DRAWN
        lottery.drawEntrants()

        val result = handler.createResultNotifications(lottery, listOf(pineRidge), createdOn)

        assertEquals(2, result.count { it.type == NotificationType.LOTTERY_WON })
        assertEquals(3, result.count { it.type == NotificationType.LOTTERY_LOST })
    }

    @Test
    fun lotteryNotClosed_createsNothing() {
        val lottery = Lottery("lottery-open", ownerId = "owner-bobby",
            campsiteIds = mutableListOf(pineRidge.id), registrationPeriod = registrationPeriod,
            reservationPeriod = reservationPeriod, stage = LotteryStatus.OPEN,
            waitingList = WaitingList(mutableListOf("entrant-priya", "entrant-noah")))

        val result = handler.createResultNotifications(lottery, listOf(pineRidge), createdOn)

        assertTrue(result.isEmpty())
    }

    @Test
    fun messagesUseCampsiteName() {
        val lottery = closedLottery(listOf("entrant-priya"), listOf("entrant-ali"))

        val result = handler.createResultNotifications(lottery, listOf(pineRidge), createdOn)

        assertTrue(result.all { it.message.contains("Pine Ridge") })
    }

    @Test
    fun notificationsHaveUniqueIdsLotteryIdAndDate() {
        val lottery = closedLottery(listOf("entrant-priya"), listOf("entrant-ali", "entrant-ben"))

        val result = handler.createResultNotifications(lottery, listOf(pineRidge), createdOn)

        assertEquals(result.size, result.map { it.id }.toSet().size)
        assertTrue(result.all { it.lotteryId == "lottery-pine-ridge" })
        assertTrue(result.all { it.createdAt == createdOn })
        assertFalse(result.any { it.read })
    }

    @Test
    fun closedLotteryWithNobody_createsNothing() {
        val lottery = closedLottery(emptyList(), emptyList())

        val result = handler.createResultNotifications(lottery, listOf(pineRidge), createdOn)

        assertTrue(result.isEmpty())
    }
}