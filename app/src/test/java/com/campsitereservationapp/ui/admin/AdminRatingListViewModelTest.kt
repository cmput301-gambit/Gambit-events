package com.campsitereservationapp.ui.admin

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.RatingRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.Entrant
import com.campsitereservationapp.model.Rating
import com.campsitereservationapp.model.User
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AdminRatingListViewModel].
 *
 * **Purpose:** checks the admin sees every rating with its campsite's name
 * and the rater's name,
 * that Remove asks for confirmation first, that cancelling keeps the rating,
 * and that confirming deletes only that rating (US 03.09.01).
 *
 * **Design rationale:** repositories backed by [MockDataSource], so nothing
 * touches Firestore.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminRatingListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var ratingRepository: RatingRepository
    private lateinit var viewModel: AdminRatingListViewModel

    private val pineRidge = Campsite("campsite-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 6)
    private val aspenHollow = Campsite("campsite-aspen-hollow", ownerId = "owner-jill", name = "Aspen Hollow", capacity = 2)

    private val spamRating = Rating("entrant-troll_campsite-pine-ridge", "entrant-troll", "campsite-pine-ridge", 0f)
    private val fairRating = Rating("entrant-priya_campsite-pine-ridge", "entrant-priya", "campsite-pine-ridge", 4f)
    private val aspenRating = Rating("entrant-noah_campsite-aspen-hollow", "entrant-noah", "campsite-aspen-hollow", 5f)
    private val orphanRating = Rating("entrant-noah_campsite-gone", "entrant-noah", "campsite-gone", 3f)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ratingRepository = RatingRepository(
            MockDataSource(listOf(spamRating, fairRating, aspenRating, orphanRating))
        )
        val campsiteRepository = CampsiteRepository(MockDataSource(listOf(pineRidge, aspenHollow)))
        // Noah has a name; the troll's profile has none; Priya's profile was deleted.
        val userRepository = UserRepository(MockDataSource(listOf(
            User("entrant-noah", name = "Noah Walters", entrant = Entrant("entrant-noah")),
            User("entrant-troll", entrant = Entrant("entrant-troll")),
        )))
        viewModel = AdminRatingListViewModel(ratingRepository, campsiteRepository, userRepository)
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

    private fun rowFor(rating: Rating) =
        viewModel.uiState.value.ratings.first { it.rating.id == rating.id }

    @Test
    fun beforeDataArrives_isLoading() = runTest {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun adminSeesEveryRatingWithItsCampsiteName() = runTest {
        startCollecting()
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.ratings.size)
        assertEquals("Pine Ridge", rowFor(spamRating).campsiteName)
        assertEquals("Aspen Hollow", rowFor(aspenRating).campsiteName)
        assertEquals(AdminRatingListViewModel.DELETED_CAMPSITE, rowFor(orphanRating).campsiteName)
    }

    @Test
    fun tappingRemove_asksFirstAndDeletesNothing() = runTest {
        startCollecting()
        advanceUntilIdle()

        viewModel.requestRemoval(rowFor(spamRating))
        advanceUntilIdle()

        assertEquals(spamRating, viewModel.uiState.value.ratingPendingRemoval?.rating)
        assertNotNull(ratingRepository.get(spamRating.id))
    }

    @Test
    fun cancelling_keepsTheRating() = runTest {
        startCollecting()
        advanceUntilIdle()

        viewModel.requestRemoval(rowFor(spamRating))
        viewModel.cancelRemoval()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.ratingPendingRemoval)
        assertNotNull(ratingRepository.get(spamRating.id))
    }

    @Test
    fun confirming_deletesOnlyThatRating() = runTest {
        startCollecting()
        advanceUntilIdle()

        viewModel.requestRemoval(rowFor(spamRating))
        viewModel.confirmRemoval()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.ratingPendingRemoval)
        assertNull(ratingRepository.get(spamRating.id))
        assertEquals(3, viewModel.uiState.value.ratings.size)
        assertEquals(4f, ratingRepository.averageRatingByCampsite("campsite-pine-ridge"), 0.001f)
    }

    @Test
    fun eachRatingShowsWhoLeftIt() = runTest {
        startCollecting()
        advanceUntilIdle()

        assertEquals("Noah Walters", rowFor(aspenRating).raterName)
        // A blank name and a deleted profile both have no name to show.
        assertNull(rowFor(spamRating).raterName)
        assertNull(rowFor(fairRating).raterName)
        assertEquals("entrant-troll", rowFor(spamRating).rating.userId)
    }
}
