package com.campsitereservationapp.data

import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.model.NotificationType
import com.campsitereservationapp.model.User
import com.campsitereservationapp.model.WaitingList
import com.campsitereservationapp.service.NotificationHandler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for [NotificationHandler].
 *
 * **Purpose:** checks that a drawn lottery creates one win or lose
 * notification per entrant, and that they are saved so each entrant
 * sees theirs (US 01.03.01, 01.03.02). Also checks owner messages to a
 * waiting list: one per entrant, sharing one batch and send time, skipping
 * opted-out entrants, and refusing invalid text (US 02.10.01).
 */
class NotificationHandlerTest {
    private val repository = NotificationRepository(MockDataSource())
    private val handler = NotificationHandler(repository)
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
        val entrants = listOf("entrant-priya", "entrant-noah", "entrant-ali", "entrant-ben", "entrant-sara")
        val drawn = lottery.copy(stage = LotteryStatus.DRAWN, waitingList = WaitingList(entrantList = entrants))
            .drawn()!!

        val result = handler.createResultNotifications(drawn, listOf(pineRidge), createdOn)


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

    @Test
    fun sendResults_savesOneNotificationPerEntrant() = runTest {
        val lottery = closedLottery(
            winners = listOf("entrant-priya", "entrant-noah"),
            losers = listOf("entrant-ali", "entrant-ben", "entrant-sara"),
        )

        handler.sendResultNotifications(lottery, listOf(pineRidge), createdOn)

        assertEquals(5, repository.getAll().size)
    }

    @Test
    fun sendResults_winnerSeesWinInTheirInbox() = runTest {
        val lottery = closedLottery(listOf("entrant-priya"), listOf("entrant-ali"))

        handler.sendResultNotifications(lottery, listOf(pineRidge), createdOn)

        val priyasInbox = repository.observeForUser("entrant-priya").first()
        assertEquals(1, priyasInbox.size)
        assertEquals(NotificationType.LOTTERY_WON, priyasInbox[0].type)
    }

    @Test
    fun sendResults_lotteryNotClosed_savesNothing() = runTest {
        val lottery = Lottery("lottery-open", ownerId = "owner-bobby",
            campsiteIds = mutableListOf(pineRidge.id), registrationPeriod = registrationPeriod,
            reservationPeriod = reservationPeriod, stage = LotteryStatus.OPEN,
            waitingList = WaitingList(mutableListOf("entrant-priya")))

        handler.sendResultNotifications(lottery, listOf(pineRidge), createdOn)

        assertTrue(repository.getAll().isEmpty())
    }

    // ---------- Owner messages to the waiting list (US 02.10.01) ----------

    private val sentAt = LocalDateTime.of(2026, 11, 27, 14, 5, 9)

    private fun openLottery(entrants: List<String>) = Lottery(
        "lottery-pine-ridge",
        ownerId = "owner-bobby",
        campsiteIds = mutableListOf(pineRidge.id),
        registrationPeriod = registrationPeriod,
        reservationPeriod = reservationPeriod,
        stage = LotteryStatus.OPEN,
        waitingList = WaitingList(entrantList = entrants),
    )

    private val priya = User(id = "entrant-priya", name = "Priya")
    private val noah = User(id = "entrant-noah", name = "Noah")
    private val ali = User(id = "entrant-ali", name = "Ali", notificationsEnabled = false)

    @Test
    fun waitlistMessage_savesOnePerEntrant_withSenderAndLottery() = runTest {
        val lottery = openLottery(listOf("entrant-priya", "entrant-noah"))

        val result = handler.sendWaitlistMessage(lottery, "owner-bobby", "Gate opens at 3pm", listOf(priya, noah), sentAt)

        val saved = repository.getAll()
        assertEquals(2, saved.size)
        assertEquals(setOf("entrant-priya", "entrant-noah"), saved.map { it.recipientId }.toSet())
        assertTrue(saved.all { it.senderId == "owner-bobby" })
        assertTrue(saved.all { it.lotteryId == "lottery-pine-ridge" })
        assertTrue(saved.all { it.type == NotificationType.OWNER_MESSAGE })
        assertTrue(saved.all { it.message == "Gate opens at 3pm" })
        assertEquals(2, result.sent.size)
        assertEquals(0, result.skippedOptedOut)
    }

    @Test
    fun waitlistMessage_oneSend_sharesBatchIdAndSendTime() = runTest {
        val lottery = openLottery(listOf("entrant-priya", "entrant-noah"))

        handler.sendWaitlistMessage(lottery, "owner-bobby", "Hi", listOf(priya, noah), sentAt)

        val saved = repository.getAll()
        val batchIds = saved.map { it.batchId }.toSet()
        assertEquals(1, batchIds.size)
        assertTrue(batchIds.single().isNotBlank())
        assertEquals(setOf(sentAt), saved.map { it.sentAt }.toSet())
        // Each notification still has its own ID.
        assertEquals(2, saved.map { it.id }.toSet().size)
    }

    @Test
    fun waitlistMessage_twoSends_getDifferentBatchIds() = runTest {
        val lottery = openLottery(listOf("entrant-priya"))

        handler.sendWaitlistMessage(lottery, "owner-bobby", "First", listOf(priya), sentAt)
        handler.sendWaitlistMessage(lottery, "owner-bobby", "Second", listOf(priya), sentAt.plusMinutes(1))

        assertEquals(2, repository.getAll().map { it.batchId }.toSet().size)
    }

    @Test
    fun waitlistMessage_skipsOptedOutEntrants() = runTest {
        val lottery = openLottery(listOf("entrant-priya", "entrant-ali"))

        val result = handler.sendWaitlistMessage(lottery, "owner-bobby", "Hi", listOf(priya, ali), sentAt)

        assertEquals(listOf("entrant-priya"), repository.getAll().map { it.recipientId })
        assertEquals(1, result.skippedOptedOut)
    }

    @Test
    fun waitlistMessage_skipsEntrantsWithNoProfile() = runTest {
        val lottery = openLottery(listOf("entrant-priya", "entrant-deleted"))

        val result = handler.sendWaitlistMessage(lottery, "owner-bobby", "Hi", listOf(priya), sentAt)

        assertEquals(listOf("entrant-priya"), repository.getAll().map { it.recipientId })
        assertEquals(0, result.skippedOptedOut)
    }

    @Test
    fun waitlistMessage_dedupesEntrants_andNeverSendsToTheSender() = runTest {
        val owner = User(id = "owner-bobby", name = "Bobby")
        val lottery = openLottery(listOf("entrant-priya", "entrant-priya", "owner-bobby"))

        handler.sendWaitlistMessage(lottery, "owner-bobby", "Hi", listOf(priya, owner), sentAt)

        assertEquals(listOf("entrant-priya"), repository.getAll().map { it.recipientId })
    }

    @Test
    fun waitlistMessage_emptyWaitlist_savesNothing() = runTest {
        val result = handler.sendWaitlistMessage(openLottery(emptyList()), "owner-bobby", "Hi", emptyList(), sentAt)

        assertTrue(result.sent.isEmpty())
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun waitlistMessage_blank_isRejected_andSavesNothing() = runTest {
        val lottery = openLottery(listOf("entrant-priya"))

        val thrown = runCatching {
            handler.sendWaitlistMessage(lottery, "owner-bobby", "   ", listOf(priya), sentAt)
        }.exceptionOrNull()

        assertTrue(thrown is IllegalArgumentException)
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun waitlistMessage_tooLong_isRejected_andSavesNothing() = runTest {
        val lottery = openLottery(listOf("entrant-priya"))
        val tooLong = "a".repeat(Notification.MAX_LENGTH + 1)

        val thrown = runCatching {
            handler.sendWaitlistMessage(lottery, "owner-bobby", tooLong, listOf(priya), sentAt)
        }.exceptionOrNull()

        assertTrue(thrown is IllegalArgumentException)
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun sendReservationRejected_savesOneNoticeForTheEntrant() = runTest {
        val reservation = Reservation(
            id = "res-1",
            entrantId = "entrant-priya",
            campsiteId = "campsite-pine-ridge",
            lotteryId = "lottery-1",
            dates = DateRange(LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 4)),
        )

        val sent = handler.sendReservationRejected(reservation, "Pine Ridge", "owner-bobby", createdOn)

        assertEquals(listOf(sent), repository.getAll())
        assertEquals("entrant-priya", sent.recipientId)
        assertEquals(NotificationType.RESERVATION_REJECTED, sent.type)
        assertEquals(createdOn, sent.createdAt)
    }
}