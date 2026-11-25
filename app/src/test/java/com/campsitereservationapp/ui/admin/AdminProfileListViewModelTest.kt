package com.campsitereservationapp.ui.admin

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MessageRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.data.RatingRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Admin
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteOwner
import com.campsitereservationapp.model.Entrant
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AdminProfileListViewModel].
 *
 * **Purpose:** checks the admin sees every profile, that both kinds of
 * removal ask for confirmation first, that cancelling keeps everything, and
 * that confirming removes the profile (US 03.02.01) or the owner's listings
 * and role (US 03.07.01). Also checks the admin can't remove themselves.
 *
 * **Design rationale:** repositories backed by [MockDataSource], so nothing
 * touches Firestore. The full cleanup rules are tested in CascadeDeleterTest;
 * these tests only check the screen's steps.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminProfileListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var viewModel: AdminProfileListViewModel

    private val sam = User("admin-sam", name = "Sam Okafor", entrant = Entrant("admin-sam"), admin = Admin("admin-sam"))
    private val priya = User("entrant-priya", name = "Priya Shah", entrant = Entrant("entrant-priya"))
    private val bobby = User(
        "owner-bobby", name = "Bobby Smith",
        entrant = Entrant("owner-bobby"), owner = CampsiteOwner("owner-bobby"),
    )
    private val pineRidge = Campsite("campsite-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 6)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = UserRepository(MockDataSource(listOf(sam, priya, bobby)))
        campsiteRepository = CampsiteRepository(MockDataSource(listOf(pineRidge)))
        val deleter = CascadeDeleter(
            userRepository,
            campsiteRepository,
            LotteryRepository(MockDataSource(), campsiteRepository),
            ReservationRepository(MockDataSource()),
            NotificationRepository(MockDataSource()),
            RatingRepository(MockDataSource()),
            MessageRepository(MockDataSource()),
        )
        viewModel = AdminProfileListViewModel(userRepository, deleter, currentUserId = "admin-sam")
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
    fun beforeDataArrives_isLoading() = runTest {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun adminSeesEveryProfile_sortedByName() = runTest {
        startCollecting()
        advanceUntilIdle()

        val names = viewModel.uiState.value.users.map { it.name }
        assertEquals(listOf("Bobby Smith", "Priya Shah", "Sam Okafor"), names)
    }

    @Test
    fun tappingRemoveProfile_asksFirstAndDeletesNothing() = runTest {
        startCollecting()
        viewModel.requestProfileRemoval(priya)
        advanceUntilIdle()

        assertEquals(ProfileRemoval.WholeProfile(priya), viewModel.uiState.value.pendingRemoval)
        assertNotNull(userRepository.get("entrant-priya"))
    }

    @Test
    fun cancelling_keepsTheProfile() = runTest {
        startCollecting()
        viewModel.requestProfileRemoval(priya)
        viewModel.cancelRemoval()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingRemoval)
        assertNotNull(userRepository.get("entrant-priya"))
    }

    @Test
    fun confirmingProfileRemoval_deletesTheProfile() = runTest {
        startCollecting()
        viewModel.requestProfileRemoval(priya)
        viewModel.confirmRemoval()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingRemoval)
        assertNull(userRepository.get("entrant-priya"))
        assertFalse(viewModel.uiState.value.users.any { it.id == "entrant-priya" })
    }

    @Test
    fun confirmingOwnerRemoval_deletesTheirCampsitesAndKeepsTheEntrant() = runTest {
        startCollecting()
        viewModel.requestOwnerRemoval(bobby)
        advanceUntilIdle()
        assertEquals(ProfileRemoval.OwnerRole(bobby), viewModel.uiState.value.pendingRemoval)

        viewModel.confirmRemoval()
        advanceUntilIdle()

        val updated = userRepository.get("owner-bobby")!!
        assertFalse(updated.isOwner)
        assertTrue(updated.isEntrant)
        assertNull(campsiteRepository.get("campsite-pine-ridge"))
    }

    @Test
    fun ownerRemoval_isIgnoredForNonOwners() = runTest {
        startCollecting()
        viewModel.requestOwnerRemoval(priya)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingRemoval)
    }

    @Test
    fun adminCannotRemoveThemselves() = runTest {
        startCollecting()
        viewModel.requestProfileRemoval(sam)
        viewModel.confirmRemoval()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingRemoval)
        assertNotNull(userRepository.get("admin-sam"))
    }

    @Test
    fun confirmWithNothingPending_doesNothing() = runTest {
        startCollecting()
        viewModel.confirmRemoval()
        advanceUntilIdle()

        assertEquals(3, userRepository.getAll().size)
    }
}
