package com.campsitereservationapp.data

import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.model.NotificationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for [NotificationRepository]'s queries.
 *
 * **Purpose:** checks [NotificationRepository.observeSentBy] returns only
 * the owner's own waitlist messages (US 02.10.02), and that
 * [NotificationRepository.observeForUser] sorts newest first using the send
 * time when there is one.
 *
 * **Design rationale:** a [MockDataSource], so nothing touches Firestore.
 *
 * **Outstanding issues:** None.
 */
class NotificationRepositoryTest {

    private fun ownerMessage(id: String, senderId: String, recipientId: String, sentAt: LocalDateTime) =
        Notification.fromOwner(id, recipientId, "lottery-pine-ridge", senderId, "Hello", "batch-$id", sentAt)

    private val bobbyToPriya = ownerMessage("n-1", "owner-bobby", "entrant-priya", LocalDateTime.of(2026, 11, 1, 9, 0))
    private val bobbyToNoah = ownerMessage("n-2", "owner-bobby", "entrant-noah", LocalDateTime.of(2026, 11, 2, 9, 0))
    private val carolToPriya = ownerMessage("n-3", "owner-carol", "entrant-priya", LocalDateTime.of(2026, 11, 3, 9, 0))
    private val priyasWin = Notification.won(
        "n-4", "entrant-priya", "lottery-pine-ridge", "Pine Ridge", LocalDate.of(2026, 11, 1))

    // Stands in for a reservation rejection (US 02.08.01), which also records the owner as sender.
    private val bobbysOtherType = Notification(
        id = "n-5",
        recipientId = "entrant-priya",
        type = NotificationType.LOTTERY_LOST,
        message = "Your reservation was rejected.",
        createdAt = LocalDate.of(2026, 11, 4),
        senderId = "owner-bobby",
    )

    private val repository = NotificationRepository(
        MockDataSource(listOf(bobbyToPriya, bobbyToNoah, carolToPriya, priyasWin, bobbysOtherType)))

    @Test
    fun observeSentBy_returnsOnlyThisOwnersMessages() = runTest {
        val sent = repository.observeSentBy("owner-bobby").first()

        assertEquals(setOf("n-1", "n-2"), sent.map { it.id }.toSet())
    }

    @Test
    fun observeSentBy_excludesOtherTypesWithTheSameSender() = runTest {
        val sent = repository.observeSentBy("owner-bobby").first()

        assertTrue(sent.all { it.type == NotificationType.OWNER_MESSAGE })
        assertTrue(sent.none { it.id == "n-5" })
    }

    @Test
    fun observeSentBy_ownerWhoSentNothing_isEmpty() = runTest {
        assertTrue(repository.observeSentBy("owner-dana").first().isEmpty())
    }

    @Test
    fun observeForUser_sameDayMessages_newestSendTimeFirst() = runTest {
        val morning = ownerMessage("m-1", "owner-bobby", "entrant-ali", LocalDateTime.of(2026, 11, 5, 8, 0))
        val evening = ownerMessage("m-2", "owner-bobby", "entrant-ali", LocalDateTime.of(2026, 11, 5, 20, 0))
        val repo = NotificationRepository(MockDataSource(listOf(morning, evening)))

        assertEquals(listOf("m-2", "m-1"), repo.observeForUser("entrant-ali").first().map { it.id })
    }

    @Test
    fun observeForUser_usesCreatedAtWhenThereIsNoSendTime() = runTest {
        val inbox = repository.observeForUser("entrant-priya").first().map { it.id }

        // n-5 (Nov 4, date only), n-3 (Nov 3 9:00), n-1 (Nov 1 9:00), n-4 (Nov 1, date only = midnight)
        assertEquals(listOf("n-5", "n-3", "n-1", "n-4"), inbox)
    }
}
