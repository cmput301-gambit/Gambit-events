package com.campsitereservationapp.ui.messages

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.DataSource
import com.campsitereservationapp.data.MessageRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Identifiable
import com.campsitereservationapp.model.Message
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.model.SenderRole
import com.campsitereservationapp.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for [ConversationViewModel].
 *
 * **Purpose:** checks that only the owner and the guest can open a
 * reservation's conversation, that the owner's messages go to the guest
 * (US 02.12.01) and the guest's go to the owner (US 01.14.01), that blank
 * messages are refused, that received messages are marked read, that a
 * double tap on Send posts only one message, and that a reservation
 * cancelled while the chat is open blocks sending.
 *
 * **Design rationale:** repositories backed by [MockDataSource], so nothing
 * touches Firestore. The double-tap test uses [SlowDataSource], because
 * [MockDataSource] saves instantly and would hide the bug.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var reservationRepository: ReservationRepository
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var userRepository: UserRepository
    private lateinit var messageRepository: MessageRepository

    private val pineRidge = Campsite(id = "campsite-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge")

    private val priyasReservation = Reservation(
        id = "reservation-priya",
        entrantId = "entrant-priya",
        campsiteId = "campsite-pine-ridge",
        lotteryId = "lottery-pine-ridge",
        dates = DateRange(LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 4)),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        reservationRepository = ReservationRepository(MockDataSource(listOf(priyasReservation)))
        campsiteRepository = CampsiteRepository(MockDataSource(listOf(pineRidge)))
        userRepository = UserRepository(
            MockDataSource(listOf(User(id = "entrant-priya", name = "Priya"), User(id = "owner-bobby", name = "Bobby")))
        )
        messageRepository = MessageRepository(MockDataSource())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(userId: String) = ConversationViewModel(
        reservationRepository, campsiteRepository, userRepository, messageRepository, userId,
    )

    private fun message(id: String, role: SenderRole, hour: Int, text: String = "hi") = Message(
        id = id,
        reservationId = "reservation-priya",
        senderId = if (role == SenderRole.OWNER) "owner-bobby" else "entrant-priya",
        recipientId = if (role == SenderRole.OWNER) "entrant-priya" else "owner-bobby",
        senderRole = role,
        text = text,
        sentAt = LocalDateTime.of(2026, 11, 24, hour, 0),
    )

    @Test
    fun owner_opensConversation_withGuestsName() = runTest {
        val viewModel = viewModelFor("owner-bobby")

        viewModel.load("reservation-priya")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.blockedReason)
        assertEquals("Priya", state.otherPartyName)
        assertEquals("Pine Ridge", state.campsiteName)
        assertTrue(state.canSend)
    }

    @Test
    fun owner_sendsMessage_toTheGuest_andDraftClears() = runTest {
        val viewModel = viewModelFor("owner-bobby")
        viewModel.load("reservation-priya")
        advanceUntilIdle()

        viewModel.onDraftChanged("Gate code is 1234")
        viewModel.send()
        advanceUntilIdle()

        val saved = messageRepository.getAll().single()
        assertEquals("owner-bobby", saved.senderId)
        assertEquals("entrant-priya", saved.recipientId)
        assertEquals(SenderRole.OWNER, saved.senderRole)
        assertEquals("", viewModel.uiState.value.draft)
        assertTrue(viewModel.uiState.value.messages.single().isMine)
    }

    @Test
    fun guest_sendsMessage_toTheOwner() = runTest {
        val viewModel = viewModelFor("entrant-priya")
        viewModel.load("reservation-priya")
        advanceUntilIdle()

        viewModel.onDraftChanged("Can I check in early?")
        viewModel.send()
        advanceUntilIdle()

        val saved = messageRepository.getAll().single()
        assertEquals("entrant-priya", saved.senderId)
        assertEquals("owner-bobby", saved.recipientId)
        assertEquals(SenderRole.ENTRANT, saved.senderRole)
        assertEquals("Bobby", viewModel.uiState.value.otherPartyName)
    }

    @Test
    fun blankDraft_isRefused() = runTest {
        val viewModel = viewModelFor("owner-bobby")
        viewModel.load("reservation-priya")
        advanceUntilIdle()

        viewModel.onDraftChanged("   ")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("Write a message first.", viewModel.uiState.value.error)
        assertTrue(messageRepository.getAll().isEmpty())
    }

    @Test
    fun someoneElse_isBlocked() = runTest {
        val viewModel = viewModelFor("entrant-ali")

        viewModel.load("reservation-priya")
        advanceUntilIdle()

        assertEquals(
            "Only the campsite owner and the guest can see this conversation.",
            viewModel.uiState.value.blockedReason,
        )
        assertFalse(viewModel.uiState.value.canSend)
    }

    @Test
    fun missingReservation_isBlocked() = runTest {
        val viewModel = viewModelFor("owner-bobby")

        viewModel.load("reservation-gone")
        advanceUntilIdle()

        assertEquals("This reservation no longer exists.", viewModel.uiState.value.blockedReason)
    }

    @Test
    fun cancelledReservation_isBlocked() = runTest {
        reservationRepository.cancel("reservation-priya")
        val viewModel = viewModelFor("owner-bobby")

        viewModel.load("reservation-priya")
        advanceUntilIdle()

        assertEquals("This reservation is no longer active.", viewModel.uiState.value.blockedReason)
    }

    @Test
    fun doubleTapSend_sendsOnlyOnce() = runTest {
        messageRepository = MessageRepository(SlowDataSource(MockDataSource()))
        val viewModel = viewModelFor("owner-bobby")
        viewModel.load("reservation-priya")
        advanceUntilIdle()

        viewModel.onDraftChanged("Gate code is 1234")
        viewModel.send()
        viewModel.send()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSending)
        assertFalse(viewModel.uiState.value.canSend)

        advanceUntilIdle()

        assertEquals(1, messageRepository.getAll().size)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSending)
        assertTrue(viewModel.uiState.value.canSend)
    }

    @Test
    fun reservationCancelledWhileOpen_blocksSending() = runTest {
        val viewModel = viewModelFor("owner-bobby")
        viewModel.load("reservation-priya")
        advanceUntilIdle()

        reservationRepository.cancel("reservation-priya")
        viewModel.onDraftChanged("See you soon")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("This reservation is no longer active.", viewModel.uiState.value.blockedReason)
        assertFalse(viewModel.uiState.value.canSend)
        assertTrue(messageRepository.getAll().isEmpty())
    }

    @Test
    fun messages_showOldestFirst_andWhoSentThem() = runTest {
        messageRepository.send(message("m-late", SenderRole.ENTRANT, hour = 11))
        messageRepository.send(message("m-early", SenderRole.OWNER, hour = 9))
        val viewModel = viewModelFor("owner-bobby")

        viewModel.load("reservation-priya")
        advanceUntilIdle()

        val shown = viewModel.uiState.value.messages
        assertEquals(listOf("m-early", "m-late"), shown.map { it.id })
        assertEquals(listOf(true, false), shown.map { it.isMine })
        assertEquals("2026-11-24 09:00", shown.first().sentAt)
    }

    @Test
    fun openingTheConversation_marksReceivedMessagesRead() = runTest {
        messageRepository.send(message("m-1", SenderRole.ENTRANT, hour = 9))
        messageRepository.send(message("m-2", SenderRole.OWNER, hour = 10))
        val viewModel = viewModelFor("owner-bobby")

        viewModel.load("reservation-priya")
        advanceUntilIdle()

        assertTrue(messageRepository.get("m-1")!!.read)
        assertFalse(messageRepository.get("m-2")!!.read)
    }
}

/**
 * A [DataSource] that waits a second before saving, like a slow network.
 * Lets tests tap Send again while the first message is still being saved.
 *
 * @param inner where items are really stored
 */
private class SlowDataSource<T : Identifiable>(
    private val inner: DataSource<T>,
) : DataSource<T> by inner {
    override suspend fun add(item: T) {
        delay(1_000)
        inner.add(item)
    }
}
