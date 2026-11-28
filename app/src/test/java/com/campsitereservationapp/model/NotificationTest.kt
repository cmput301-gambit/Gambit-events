package com.campsitereservationapp.model

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Notification].
 *
 * **Purpose:** checks win/lose creation, read state and delivery rules
 * (US 01.03.01, 01.03.02).
 */
class NotificationTest {

    private val entrant = User.newEntrant("device-123")
    private val createdOn = LocalDate.of(2026, 10, 28)

    @Test
    fun won_createsUnreadWinNotificationWithCampsiteName() {
        val n = Notification.won("n1", "device-123", "lottery-1", "Lakeview Camp", createdOn)

        assertEquals(NotificationType.LOTTERY_WON, n.type)
        assertEquals("device-123", n.recipientId)
        assertEquals(createdOn, n.createdAt)
        assertFalse(n.read)
        assertTrue(n.message.contains("Lakeview Camp"))
    }

    @Test
    fun lost_createsUnreadLoseNotification() {
        val n = Notification.lost("n2", "device-123", "lottery-1", "Lakeview Camp", createdOn)

        assertEquals(NotificationType.LOTTERY_LOST, n.type)
        assertFalse(n.read)
        assertTrue(n.message.contains("Lakeview Camp"))
    }

    @Test
    fun markRead_returnsReadCopyAndLeavesOriginalUnchanged() {
        val original = Notification.won("n1", "device-123", "lottery-1", "Lakeview Camp", createdOn)

        val updated = original.markRead()

        assertTrue(updated.read)
        assertFalse(original.read)
    }

    @Test
    fun canBeDeliveredTo_trueForRecipientWithNotificationsOn() {
        val n = Notification.won("n1", "device-123", "lottery-1", "Lakeview Camp", createdOn)

        assertTrue(n.canBeDeliveredTo(entrant))
    }

    @Test
    fun canBeDeliveredTo_falseWhenUserOptedOut() {
        val n = Notification.won("n1", "device-123", "lottery-1", "Lakeview Camp", createdOn)

        assertFalse(n.canBeDeliveredTo(entrant.copy(notificationsEnabled = false)))
    }

    @Test
    fun canBeDeliveredTo_falseForADifferentUser() {
        val n = Notification.won("n1", "device-123", "lottery-1", "Lakeview Camp", createdOn)

        assertFalse(n.canBeDeliveredTo(User.newEntrant("device-999")))
    }

    // ---------- Owner messages (US 02.10.01) ----------

    private val sentAt = LocalDateTime.of(2026, 11, 27, 14, 5, 9, 123_456_789)

    private fun ownerMessage(
        text: String = "Gate opens at 3pm",
        recipientId: String = "entrant-priya",
        senderId: String = "owner-bobby",
    ) = Notification.fromOwner("n1", recipientId, "lottery-1", senderId, text, "batch-1", sentAt)

    @Test
    fun fromOwner_setsSenderLotteryBatchAndSendTime() {
        val n = ownerMessage()

        assertEquals(NotificationType.OWNER_MESSAGE, n.type)
        assertEquals("entrant-priya", n.recipientId)
        assertEquals("lottery-1", n.lotteryId)
        assertEquals("owner-bobby", n.senderId)
        assertEquals("batch-1", n.batchId)
        assertEquals(LocalDateTime.of(2026, 11, 27, 14, 5, 9), n.sentAt)
        assertEquals(LocalDate.of(2026, 11, 27), n.createdAt)
        assertFalse(n.read)
        assertTrue(n.isValid())
    }

    @Test
    fun fromOwner_trimsText() {
        assertEquals("Hello", ownerMessage(text = "  Hello \n").message)
    }

    @Test
    fun winAndLose_leaveOwnerFieldsEmpty() {
        val n = Notification.lost("n1", "device-123", "lottery-1", "Lakeview Camp", createdOn)

        assertEquals("", n.senderId)
        assertEquals("", n.batchId)
        assertNull(n.sentAt)
        assertTrue(n.isValid())
    }

    @Test
    fun ownerMessage_blank_isInvalid() {
        assertEquals(listOf(NotificationError.BLANK_MESSAGE), ownerMessage(text = "   ").validate())
    }

    @Test
    fun ownerMessage_atMaxLength_isValid() {
        assertTrue(ownerMessage(text = "a".repeat(Notification.MAX_LENGTH)).isValid())
    }

    @Test
    fun ownerMessage_overMaxLength_isInvalid() {
        assertEquals(
            listOf(NotificationError.MESSAGE_TOO_LONG),
            ownerMessage(text = "a".repeat(Notification.MAX_LENGTH + 1)).validate(),
        )
    }

    @Test
    fun ownerMessage_withoutSender_isInvalid() {
        assertEquals(listOf(NotificationError.BLANK_SENDER), ownerMessage(senderId = "").validate())
    }

    @Test
    fun ownerMessage_toSelf_isInvalid() {
        assertEquals(listOf(NotificationError.SENT_TO_SELF), ownerMessage(recipientId = "owner-bobby").validate())
    }

    @Test
    fun reservationRejected_isAValidNoticeToTheEntrantFromTheOwner() {
        val reservation = Reservation(
            id = "res-1",
            entrantId = "entrant-priya",
            campsiteId = "campsite-pine-ridge",
            lotteryId = "lottery-1",
            dates = DateRange(LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 4)),
        )

        val notice = Notification.reservationRejected(
            "n-1", reservation, "Pine Ridge", "owner-bobby", LocalDate.of(2026, 11, 27)
        )

        assertEquals("entrant-priya", notice.recipientId)
        assertEquals("owner-bobby", notice.senderId)
        assertEquals("lottery-1", notice.lotteryId)
        assertEquals(NotificationType.RESERVATION_REJECTED, notice.type)
        assertTrue(notice.message.contains("Pine Ridge"))
        assertTrue(notice.message.contains("2026-12-02"))
        assertFalse(notice.read)
        assertTrue(notice.isValid())
    }
}