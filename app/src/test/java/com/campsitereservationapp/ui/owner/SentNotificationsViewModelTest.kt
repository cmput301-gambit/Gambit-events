package com.campsitereservationapp.ui.owner

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.model.WaitingList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for [SentNotificationsViewModel].
 *
 * **Purpose:** checks the owner sees one row per send with the right
 * campsite names and recipient count, newest first, only their own sends,
 * an empty state when they sent nothing, and "Deleted lottery" when the
 * lottery is gone (US 02.10.02).
 *
 * **Design rationale:** repositories backed by [MockDataSource], so nothing
 * touches Firestore.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SentNotificationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var lotteryRepository: LotteryRepository
    private lateinit var campsiteRepository: CampsiteRepository

    private val pineRidge = Campsite(id = "campsite-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge")
    private val lakeside = Campsite(id = "campsite-lakeside", ownerId = "owner-bobby", name = "Lakeside Loop")

    private fun lottery(id: String, campsiteIds: List<String>, ownerId: String = "owner-bobby") = Lottery(
        id,
        ownerId = ownerId,
        campsiteIds = campsiteIds.toMutableList(),
        registrationPeriod = DateRange(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5)),
        reservationPeriod = DateRange(LocalDate.now().plusDays(6), LocalDate.now().plusDays(8)),
        stage = LotteryStatus.OPEN,
        waitingList = WaitingList(entrantList = listOf("entrant-priya", "entrant-noah")),
    )

    // One send = one notification per recipient, all sharing a batch ID.
    private fun send(
        batchId: String,
        lotteryId: String,
        recipients: List<String>,
        sentAt: LocalDateTime,
        text: String = "Hello from $batchId",
        senderId: String = "owner-bobby",
    ): List<Notification> = recipients.map { recipient ->
        Notification.fromOwner("$batchId-$recipient", recipient, lotteryId, senderId, text, batchId, sentAt)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepository = CampsiteRepository(MockDataSource(listOf(pineRidge, lakeside)))
        lotteryRepository = LotteryRepository(
            MockDataSource(
                listOf(
                    lottery("lottery-pine", listOf("campsite-pine-ridge")),
                    lottery("lottery-both", listOf("campsite-pine-ridge", "campsite-lakeside")),
                    lottery("lottery-carol", listOf("campsite-lakeside"), ownerId = "owner-carol"),
                )
            ),
            campsiteRepository,
        )
        notificationRepository = NotificationRepository(MockDataSource())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(userId: String) =
        SentNotificationsViewModel(notificationRepository, lotteryRepository, campsiteRepository, userId)

    private fun TestScope.startCollecting(viewModel: SentNotificationsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    private suspend fun save(notifications: List<Notification>) {
        notifications.forEach { notificationRepository.add(it) }
    }

    @Test
    fun oneRowPerSend_withRecipientCountAndCampsites() = runTest {
        save(send("batch-1", "lottery-both", listOf("entrant-priya", "entrant-noah", "entrant-ali"),
            LocalDateTime.of(2026, 11, 20, 14, 5), text = "Gates open at 9"))
        val viewModel = viewModelFor("owner-bobby")
        startCollecting(viewModel)
        advanceUntilIdle()

        val sends = viewModel.uiState.value.sends
        assertEquals(1, sends.size)
        val row = sends.single()
        assertEquals("Gates open at 9", row.message)
        assertEquals("Pine Ridge, Lakeside Loop", row.campsiteNames)
        assertEquals(3, row.recipientCount)
        assertEquals(LocalDateTime.of(2026, 11, 20, 14, 5), row.sentAt)
        assertFalse(row.lotteryDeleted)
    }

    @Test
    fun sends_newestFirst_includingSameDay() = runTest {
        save(send("batch-old", "lottery-pine", listOf("entrant-priya"), LocalDateTime.of(2026, 11, 1, 9, 0)))
        save(send("batch-morning", "lottery-pine", listOf("entrant-priya"), LocalDateTime.of(2026, 11, 20, 8, 0)))
        save(send("batch-evening", "lottery-both", listOf("entrant-noah"), LocalDateTime.of(2026, 11, 20, 19, 30)))
        val viewModel = viewModelFor("owner-bobby")
        startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(
            listOf("batch-evening", "batch-morning", "batch-old"),
            viewModel.uiState.value.sends.map { it.batchId },
        )
    }

    @Test
    fun ownerWhoSentNothing_seesEmptyState() = runTest {
        val viewModel = viewModelFor("owner-bobby")
        startCollecting(viewModel)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.sends.isEmpty())
    }

    @Test
    fun beforeDataArrives_isLoading() {
        val viewModel = viewModelFor("owner-bobby")

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun showsOnlyMyOwnSends() = runTest {
        save(send("batch-bobby", "lottery-pine", listOf("entrant-priya"), LocalDateTime.of(2026, 11, 20, 9, 0)))
        save(send("batch-carol", "lottery-carol", listOf("entrant-priya"), LocalDateTime.of(2026, 11, 21, 9, 0),
            senderId = "owner-carol"))

        val bobby = viewModelFor("owner-bobby")
        val carol = viewModelFor("owner-carol")
        startCollecting(bobby)
        startCollecting(carol)
        advanceUntilIdle()

        assertEquals(listOf("batch-bobby"), bobby.uiState.value.sends.map { it.batchId })
        assertEquals(listOf("batch-carol"), carol.uiState.value.sends.map { it.batchId })
    }

    @Test
    fun deletedLottery_stillShown_asDeletedLottery() = runTest {
        save(send("batch-gone", "lottery-pine", listOf("entrant-priya", "entrant-noah"),
            LocalDateTime.of(2026, 11, 20, 9, 0)))
        val viewModel = viewModelFor("owner-bobby")
        startCollecting(viewModel)
        advanceUntilIdle()

        lotteryRepository.delete("lottery-pine")
        advanceUntilIdle()

        val row = viewModel.uiState.value.sends.single()
        assertEquals(SentNotificationsViewModel.DELETED_LOTTERY, row.campsiteNames)
        assertTrue(row.lotteryDeleted)
        assertEquals(2, row.recipientCount)
    }

    @Test
    fun newSend_appearsWithoutRefreshing() = runTest {
        val viewModel = viewModelFor("owner-bobby")
        startCollecting(viewModel)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.sends.isEmpty())

        save(send("batch-new", "lottery-pine", listOf("entrant-priya"), LocalDateTime.of(2026, 11, 20, 9, 0)))
        advanceUntilIdle()

        assertEquals(listOf("batch-new"), viewModel.uiState.value.sends.map { it.batchId })
    }
}
