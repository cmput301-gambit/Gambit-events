package com.campsitereservationapp.data

import com.campsitereservationapp.model.Message
import com.campsitereservationapp.model.SenderRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for [MessageRepository].
 *
 * **Purpose:** checks that valid messages are sent and invalid ones are
 * refused, that each conversation only shows its own reservation's messages
 * in order, that the admin log only holds owner messages, and that read
 * status is tracked (US 02.12.01, 01.14.01, 03.12.01).
 *
 * **Design rationale:** a fresh repository backed by a [MockDataSource] for
 * each test, so nothing touches Firestore.
 *
 * **Outstanding issues:** None.
 */
class MessageRepositoryTest {

    private lateinit var repository: MessageRepository

    private fun at(hour: Int) = LocalDateTime.of(2026, 11, 24, hour, 0)

    private fun message(
        id: String,
        reservationId: String = "reservation-priya",
        role: SenderRole = SenderRole.OWNER,
        hour: Int = 9,
        text: String = "hello",
    ) = Message(
        id = id,
        reservationId = reservationId,
        senderId = if (role == SenderRole.OWNER) "owner-bobby" else "entrant-priya",
        recipientId = if (role == SenderRole.OWNER) "entrant-priya" else "owner-bobby",
        senderRole = role,
        text = text,
        sentAt = at(hour),
    )

    @Before
    fun setUp() {
        repository = MessageRepository(MockDataSource())
    }

    @Test
    fun send_validMessage_isSaved() = runTest {
        val sent = repository.send(message("m-1"))

        assertNotNull(sent)
        assertEquals(1, repository.getAll().size)
    }

    @Test
    fun send_blankMessage_isRefused() = runTest {
        val sent = repository.send(message("m-1", text = "   "))

        assertNull(sent)
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun conversation_onlyShowsThatReservation_oldestFirst() = runTest {
        repository.send(message("m-late", hour = 11))
        repository.send(message("m-early", role = SenderRole.ENTRANT, hour = 9))
        repository.send(message("m-other", reservationId = "reservation-ali", hour = 10))

        val conversation = repository.observeForReservation("reservation-priya").first()

        assertEquals(listOf("m-early", "m-late"), conversation.map { it.id })
    }

    @Test
    fun ownerMessageLog_onlyHasOwnerMessages_newestFirst() = runTest {
        repository.send(message("m-owner-1", hour = 9))
        repository.send(message("m-entrant", role = SenderRole.ENTRANT, hour = 10))
        repository.send(message("m-owner-2", reservationId = "reservation-ali", hour = 11))

        val log = repository.observeOwnerMessageLog().first()

        assertEquals(listOf("m-owner-2", "m-owner-1"), log.map { it.id })
    }

    @Test
    fun unreadCount_countsOnlyThatUsersUnreadMessages() = runTest {
        repository.send(message("m-1"))
        repository.send(message("m-2", hour = 10))
        repository.send(message("m-3", role = SenderRole.ENTRANT))

        assertEquals(2, repository.observeUnreadCount("entrant-priya").first())
        assertEquals(1, repository.observeUnreadCount("owner-bobby").first())
    }

    @Test
    fun markRead_savesReadStatus() = runTest {
        val sent = repository.send(message("m-1"))!!

        repository.markRead(sent)

        assertTrue(repository.get("m-1")!!.read)
        assertEquals(0, repository.observeUnreadCount("entrant-priya").first())
    }

    @Test
    fun firestoreMapping_roundTrips() {
        val original = message("m-1", role = SenderRole.ENTRANT, hour = 14).markRead()

        val rebuilt = MessageRepository.fromMap("m-1", MessageRepository.toMap(original))

        assertEquals(original, rebuilt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromMap_unknownRole_throws() {
        val data = MessageRepository.toMap(message("m-1")) + ("senderRole" to "ADMIN")

        MessageRepository.fromMap("m-1", data)
    }
}