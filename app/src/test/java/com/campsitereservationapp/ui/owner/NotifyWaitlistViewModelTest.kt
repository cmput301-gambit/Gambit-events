package com.campsitereservationapp.ui.owner

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.DataSource
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Identifiable
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.model.NotificationType
import com.campsitereservationapp.model.User
import com.campsitereservationapp.model.WaitingList
import com.campsitereservationapp.service.NotificationHandler
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

/**
 * Unit tests for [NotifyWaitlistViewModel].
 *
 * **Purpose:** checks that only the lottery's owner can message its waiting
 * list, that blank and too-long messages are refused, that an empty
 * waitlist or a waitlist where everyone opted out says so, that a send
 * saves one notification per entrant with the sender, lottery and a shared
 * batch, and that a double tap on Send sends one batch (US 02.10.01).
 *
 * **Design rationale:** repositories backed by [MockDataSource], so nothing
 * touches Firestore. The double-tap test uses [SlowDataSource], because
 * [MockDataSource] saves instantly and would hide the bug.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotifyWaitlistViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var lotteryRepository: LotteryRepository
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var userRepository: UserRepository
    private lateinit var notificationRepository: NotificationRepository

    private val pineRidge = Campsite(id = "campsite-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge")

    private fun lottery(id: String, entrants: List<String>, ownerId: String = "owner-bobby") = Lottery(
        id,
        ownerId = ownerId,
        campsiteIds = mutableListOf("campsite-pine-ridge"),
        registrationPeriod = DateRange(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5)),
        reservationPeriod = DateRange(LocalDate.now().plusDays(6), LocalDate.now().plusDays(8)),
        stage = LotteryStatus.OPEN,
        waitingList = WaitingList(entrantList = entrants),
    )

    private val users = listOf(
        User(id = "owner-bobby", name = "Bobby"),
        User(id = "entrant-priya", name = "Priya"),
        User(id = "entrant-noah", name = "Noah"),
        User(id = "entrant-ali", name = "Ali", notificationsEnabled = false),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepository = CampsiteRepository(MockDataSource(listOf(pineRidge)))
        lotteryRepository = LotteryRepository(
            MockDataSource(
                listOf(
                    lottery("lottery-full", listOf("entrant-priya", "entrant-noah", "entrant-ali")),
                    lottery("lottery-empty", emptyList()),
                    lottery("lottery-opted-out", listOf("entrant-ali")),
                    lottery("lottery-closed", listOf("entrant-priya")).copy(stage = LotteryStatus.CLOSED),
                )
            ),
            campsiteRepository,
        )
        userRepository = UserRepository(MockDataSource(users))
        notificationRepository = NotificationRepository(MockDataSource())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(userId: String) = NotifyWaitlistViewModel(
        lotteryRepository,
        campsiteRepository,
        userRepository,
        NotificationHandler(notificationRepository),
        userId,
    )

    private fun loaded(userId: String, lotteryId: String): NotifyWaitlistViewModel =
        viewModelFor(userId).also { it.load(lotteryId) }

    @Test
    fun owner_opensScreen_withCampsiteAndWaitlistCount() = runTest {
        val viewModel = loaded("owner-bobby", "lottery-full")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.blockedReason)
        assertEquals("Pine Ridge", state.campsiteNames)
        assertEquals(3, state.waitlistCount)
        assertTrue(state.canSend)
    }

    @Test
    fun nonOwner_isBlocked_andCantSend() = runTest {
        val viewModel = loaded("entrant-priya", "lottery-full")
        advanceUntilIdle()

        viewModel.onDraftChanged("Hello")
        viewModel.send()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Only this lottery's owner can notify its waitlist.", state.blockedReason)
        assertFalse(state.canSend)
        assertTrue(notificationRepository.getAll().isEmpty())
    }

    @Test
    fun missingLottery_isBlocked() = runTest {
        val viewModel = loaded("owner-bobby", "lottery-does-not-exist")
        advanceUntilIdle()

        assertEquals("This lottery no longer exists.", viewModel.uiState.value.blockedReason)
    }

    @Test
    fun blankMessage_isRefused() = runTest {
        val viewModel = loaded("owner-bobby", "lottery-full")
        advanceUntilIdle()

        viewModel.onDraftChanged("   ")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("Write a message first.", viewModel.uiState.value.error)
        assertTrue(notificationRepository.getAll().isEmpty())
    }

    @Test
    fun tooLongMessage_isRefused_andDraftKept() = runTest {
        val viewModel = loaded("owner-bobby", "lottery-full")
        advanceUntilIdle()
        val tooLong = "a".repeat(Notification.MAX_LENGTH + 1)

        viewModel.onDraftChanged(tooLong)
        viewModel.send()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Messages can be up to ${Notification.MAX_LENGTH} characters.", state.error)
        assertEquals(tooLong, state.draft)
        assertTrue(notificationRepository.getAll().isEmpty())
    }

    @Test
    fun emptyWaitlist_saysSo_andSendsNothing() = runTest {
        val viewModel = loaded("owner-bobby", "lottery-empty")
        advanceUntilIdle()

        viewModel.onDraftChanged("Hello")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(
            "No one is on this waitlist yet, so there's no one to notify.",
            viewModel.uiState.value.error,
        )
        assertTrue(notificationRepository.getAll().isEmpty())
    }

    @Test
    fun everyoneOptedOut_saysSo_andSendsNothing() = runTest {
        val viewModel = loaded("owner-bobby", "lottery-opted-out")
        advanceUntilIdle()

        viewModel.onDraftChanged("Hello")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("Everyone on this waitlist has turned off notifications.", viewModel.uiState.value.error)
        assertTrue(notificationRepository.getAll().isEmpty())
    }

    @Test
    fun owner_sends_toEveryoneWithNotificationsOn_andDraftClears() = runTest {
        val viewModel = loaded("owner-bobby", "lottery-full")
        advanceUntilIdle()

        viewModel.onDraftChanged("  Gate opens at 3pm  ")
        viewModel.send()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Sent to 2 entrants (1 skipped: notifications off).", state.result)
        assertNull(state.error)
        assertEquals("", state.draft)
        assertFalse(state.isSending)

        val saved = notificationRepository.getAll()
        assertEquals(setOf("entrant-priya", "entrant-noah"), saved.map { it.recipientId }.toSet())
        assertTrue(saved.all { it.type == NotificationType.OWNER_MESSAGE })
        assertTrue(saved.all { it.message == "Gate opens at 3pm" })
        assertTrue(saved.all { it.senderId == "owner-bobby" })
        assertTrue(saved.all { it.lotteryId == "lottery-full" })
        assertEquals(1, saved.map { it.batchId }.toSet().size)
        assertEquals(1, saved.map { it.sentAt }.toSet().size)
    }

    @Test
    fun owner_canSend_atAnyStage() = runTest {
        val viewModel = loaded("owner-bobby", "lottery-closed")
        advanceUntilIdle()

        viewModel.onDraftChanged("Thanks for entering")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("Sent to 1 entrant.", viewModel.uiState.value.result)
        assertEquals(1, notificationRepository.getAll().size)
    }

    @Test
    fun lotteryDeletedAfterOpening_blocksSend() = runTest {
        val viewModel = loaded("owner-bobby", "lottery-full")
        advanceUntilIdle()
        lotteryRepository.delete("lottery-full")

        viewModel.onDraftChanged("Hello")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("This lottery no longer exists.", viewModel.uiState.value.blockedReason)
        assertTrue(notificationRepository.getAll().isEmpty())
    }

    @Test
    fun ownerChangedAfterOpening_blocksSend() = runTest {
        val viewModel = loaded("owner-bobby", "lottery-full")
        advanceUntilIdle()
        val current = lotteryRepository.get("lottery-full")!!
        lotteryRepository.update(current.copy(ownerId = "owner-someone-else"))

        viewModel.onDraftChanged("Hello")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("Only this lottery's owner can notify its waitlist.", viewModel.uiState.value.blockedReason)
        assertTrue(notificationRepository.getAll().isEmpty())
    }

    @Test
    fun doubleTap_sendsOneBatch() = runTest {
        notificationRepository = NotificationRepository(SlowDataSource(MockDataSource()))
        val viewModel = loaded("owner-bobby", "lottery-full")
        advanceUntilIdle()

        viewModel.onDraftChanged("Gate opens at 3pm")
        viewModel.send()
        viewModel.send()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSending)
        assertFalse(viewModel.uiState.value.canSend)

        advanceUntilIdle()

        val saved = notificationRepository.getAll()
        assertEquals(2, saved.size)
        assertEquals(1, saved.map { it.batchId }.toSet().size)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun editingDraft_clearsPreviousResultAndError() = runTest {
        val viewModel = loaded("owner-bobby", "lottery-full")
        advanceUntilIdle()
        viewModel.send()
        advanceUntilIdle()
        assertEquals("Write a message first.", viewModel.uiState.value.error)

        viewModel.onDraftChanged("H")

        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.result)
    }
}

/**
 * A [DataSource] that waits before saving, so a test can tap Send again
 * while the first send is still running.
 */
private class SlowDataSource<T : Identifiable>(
    private val inner: DataSource<T>,
) : DataSource<T> by inner {
    override suspend fun add(item: T) {
        delay(1_000)
        inner.add(item)
    }
}
