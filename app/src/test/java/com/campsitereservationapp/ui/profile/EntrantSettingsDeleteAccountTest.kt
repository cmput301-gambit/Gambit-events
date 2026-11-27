package com.campsitereservationapp.ui.profile

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MessageRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.data.RatingRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Entrant
import com.campsitereservationapp.model.Rating
import com.campsitereservationapp.model.User
import com.campsitereservationapp.service.CascadeDeleter
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for deleting your own account from [EntrantSettingsViewModel] (US 01.08.01).
 *
 * **Purpose:** checks that Delete Account asks first, that cancelling keeps
 * the profile, and that confirming deletes the profile and its data and
 * tells the screen to leave.
 *
 * **Design rationale:** kept separate from the existing settings tests so
 * they don't need a [CascadeDeleter]. Repositories are backed by
 * [MockDataSource], so nothing touches Firestore. The full cleanup rules are
 * tested in CascadeDeleterTest.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EntrantSettingsDeleteAccountTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var ratingRepository: RatingRepository
    private lateinit var viewModel: EntrantSettingsViewModel

    private val priya = User("entrant-priya", name = "Priya Shah", entrant = Entrant("entrant-priya"))
    private val noah = User("entrant-noah", name = "Noah Walters", entrant = Entrant("entrant-noah"))
    private val priyaRating = Rating("entrant-priya_campsite-pine-ridge", "entrant-priya", "campsite-pine-ridge", 4f)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = UserRepository(MockDataSource(listOf(priya, noah)))
        ratingRepository = RatingRepository(MockDataSource(listOf(priyaRating)))
        val campsiteRepository = CampsiteRepository(MockDataSource())
        val deleter = CascadeDeleter(
            userRepository,
            campsiteRepository,
            LotteryRepository(MockDataSource(), campsiteRepository),
            ReservationRepository(MockDataSource()),
            NotificationRepository(MockDataSource()),
            ratingRepository,
            MessageRepository(MockDataSource()),
            ImageRepository(MockDataSource()),
        )
        viewModel = EntrantSettingsViewModel(userRepository, "entrant-priya", deleter)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.startCollecting() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    @Test
    fun tappingDelete_asksFirstAndDeletesNothing() = runTest {
        startCollecting()
        viewModel.requestDeleteAccount()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeleteConfirmOpen)
        assertNotNull(userRepository.get("entrant-priya"))
    }

    @Test
    fun cancelling_keepsTheProfile() = runTest {
        startCollecting()
        viewModel.requestDeleteAccount()
        viewModel.cancelDeleteAccount()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleteConfirmOpen)
        assertFalse(viewModel.uiState.value.accountDeleted)
        assertNotNull(userRepository.get("entrant-priya"))
    }

    @Test
    fun confirming_deletesTheProfileAndItsData() = runTest {
        startCollecting()
        advanceUntilIdle()
        viewModel.requestDeleteAccount()
        viewModel.confirmDeleteAccount()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.accountDeleted)
        assertFalse(viewModel.uiState.value.isDeleteConfirmOpen)
        assertNull(userRepository.get("entrant-priya"))
        assertNull(ratingRepository.get(priyaRating.id))
        // Other people are untouched.
        assertNotNull(userRepository.get("entrant-noah"))
    }
}
