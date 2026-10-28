package com.campsitereservationapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun won_createsUnreadWinNotificationWithCampsiteName() {
        val n = Notification.won("n1", "device-123", "lottery-1", "Lakeview Camp", 1000L)

        assertEquals(NotificationType.LOTTERY_WON, n.type)
        assertEquals("device-123", n.recipientId)
        assertFalse(n.read)
        assertTrue(n.message.contains("Lakeview Camp"))
    }

    @Test
    fun lost_createsUnreadLoseNotification() {
        val n = Notification.lost("n2", "device-123", "lottery-1", "Lakeview Camp", 2000L)

        assertEquals(NotificationType.LOTTERY_LOST, n.type)
        assertFalse(n.read)
        assertTrue(n.message.contains("Lakeview Camp"))
    }

    @Test
    fun markRead_returnsReadCopyAndLeavesOriginalUnchanged() {
        val original = Notification.won("n1", "device-123", "lottery-1", "Lakeview Camp", 1000L)

        val updated = original.markRead()

        assertTrue(updated.read)
        assertFalse(original.read)
    }

    @Test
    fun canBeDeliveredTo_trueForRecipientWithNotificationsOn() {
        val n = Notification.won("n1", "device-123", "lottery-1", "Lakeview Camp", 1000L)

        assertTrue(n.canBeDeliveredTo(entrant))
    }

    @Test
    fun canBeDeliveredTo_falseWhenUserOptedOut() {
        val n = Notification.won("n1", "device-123", "lottery-1", "Lakeview Camp", 1000L)

        assertFalse(n.canBeDeliveredTo(entrant.copy(notificationsEnabled = false)))
    }

    @Test
    fun canBeDeliveredTo_falseForADifferentUser() {
        val n = Notification.won("n1", "device-123", "lottery-1", "Lakeview Camp", 1000L)

        assertFalse(n.canBeDeliveredTo(User.newEntrant("device-999")))
    }
}