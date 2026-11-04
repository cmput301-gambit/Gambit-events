package com.campsitereservationapp.ui.entrant

import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.ui.discovery.InboxViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [InboxViewModel].
 *
 * **Purpose:** checks the inbox shows only the current user's notifications,
 * newest first, counts unread ones, and marks one as read when tapped
 * (US 01.03.01, 01.03.02).
 *
 * **Design rationale:** a [NotificationRepository] backed by a
 * [MockDataSource], so nothing touches Firestore.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: NotificationRepository

    private val priyasOlderWin = Notification.won(
        "n-1", "entrant-priya", "lottery-pine-ridge", "Pine Ridge", LocalDate.of(2026, 10, 30))
    private val priyasNewerLoss = Notification.lost(
        "n-2", "entrant-priya", "lottery-lakeside", "Lakeside Loop", LocalDate.of(2026, 11, 2))
    private val alisWin = Notification.won(
        "n-3", "entrant-ali", "lottery-pine-ridge", "Pine Ridge", LocalDate.of(2026, 10, 30))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = NotificationRepository(
            MockDataSource(listOf(priyasOlderWin, priyasNewerLoss, alisWin)))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.startCollecting(viewModel: InboxViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    @Test
    fun showsOnlyMyNotifications_newestFirst() = runTest {
        val viewModel = InboxViewModel(repository, "entrant-priya")
        startCollecting(viewModel)
        advanceUntilIdle()

        val shown = viewModel.uiState.value.notifications.map { it.id }
        assertEquals(listOf("n-2", "n-1"), shown)
    }

    @Test
    fun countsUnreadNotifications() = runTest {
        val viewModel = InboxViewModel(repository, "entrant-priya")
        startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.unreadCount)
    }

    @Test
    fun tappingANotification_marksItRead() = runTest {
        val viewModel = InboxViewModel(repository, "entrant-priya")
        startCollecting(viewModel)
        advanceUntilIdle()

        viewModel.onNotificationTapped(priyasOlderWin)
        advanceUntilIdle()

        assertTrue(repository.get("n-1")!!.read)
        assertEquals(1, viewModel.uiState.value.unreadCount)
    }

    @Test
    fun userWithNoNotifications_seesEmptyInbox() = runTest {
        val viewModel = InboxViewModel(repository, "entrant-ben")
        startCollecting(viewModel)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notifications.isEmpty())
        assertEquals(0, viewModel.uiState.value.unreadCount)
    }
}