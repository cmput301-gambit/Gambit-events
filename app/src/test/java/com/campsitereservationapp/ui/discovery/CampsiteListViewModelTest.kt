package com.campsitereservationapp.ui.discovery

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.RatingRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.Image
import com.campsitereservationapp.model.ImageKind
import com.campsitereservationapp.model.Rating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CampsiteListViewModel].
 *
 * **Purpose:** checks the search screen gets each campsite's poster and
 * average rating, and that the name search still filters the list
 * (US 01.01.01, 01.13.01).
 *
 * **Design rationale:** fresh repositories backed by [MockDataSource] for each
 * test, so nothing touches Firestore.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CampsiteListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var ratingRepository: RatingRepository
    private lateinit var imageRepository: ImageRepository
    private lateinit var viewModel: CampsiteListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepository = CampsiteRepository(MockDataSource())
        ratingRepository = RatingRepository(MockDataSource())
        imageRepository = ImageRepository(MockDataSource())
        viewModel = CampsiteListViewModel(campsiteRepository, ratingRepository, imageRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun seed() {
        campsiteRepository.add(
            Campsite(id = "pine", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 6, posterImageId = "img-pine")
        )
        campsiteRepository.add(Campsite(id = "aspen", ownerId = "owner-bobby", name = "Aspen Hollow", capacity = 2))
        imageRepository.add(
            Image(id = "img-pine", uploaderId = "owner-bobby", kind = ImageKind.CAMPSITE, targetId = "pine", data = "UElORQ==")
        )
        ratingRepository.add(Rating(id = "r1", userId = "u1", campsiteId = "pine", score = 4f))
        ratingRepository.add(Rating(id = "r2", userId = "u2", campsiteId = "pine", score = 5f, review = "Lovely"))
    }

    @Test
    fun cards_getTheirPosterAndAverageRating() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        seed()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("UElORQ==", state.posters["pine"])
        assertNull(state.posters["aspen"])
        assertEquals(4.5f, state.campsiteRatings["pine"]!!, 0.001f)
        assertNull(state.campsiteRatings["aspen"])
    }

    @Test
    fun profilePictures_areNotUsedAsPosters() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        campsiteRepository.add(
            Campsite(id = "pine", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 6, posterImageId = "img-face")
        )
        imageRepository.add(
            Image(id = "img-face", uploaderId = "owner-bobby", kind = ImageKind.PROFILE, targetId = "owner-bobby", data = "RkFDRQ==")
        )
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.posters["pine"])
    }

    @Test
    fun searchingByName_filtersTheCards() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        seed()
        viewModel.onFilterName("aspen")
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("aspen"), viewModel.uiState.value.campsites.map { it.id })
    }
}
