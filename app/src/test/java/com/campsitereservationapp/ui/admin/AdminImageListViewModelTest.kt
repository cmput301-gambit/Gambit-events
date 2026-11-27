package com.campsitereservationapp.ui.admin

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MessageRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.data.RatingRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteOwner
import com.campsitereservationapp.model.Entrant
import com.campsitereservationapp.model.Image
import com.campsitereservationapp.model.ImageKind
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
 * Unit tests for [AdminImageListViewModel].
 *
 * **Purpose:** checks the admin sees every uploaded image with the uploader's
 * name and user ID, in a stable order, and that the list stays live
 * (US 03.06.01). Also checks that Remove asks for confirmation first, that
 * cancelling keeps the image, and that confirming deletes only that image and
 * clears it from its campsite (US 03.03.01).
 *
 * **Design rationale:** repositories backed by [MockDataSource], so nothing
 * touches Firestore. The image data is short made-up Base64 text, since the
 * ViewModel never decodes it.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminImageListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var imageRepository: ImageRepository
    private lateinit var viewModel: AdminImageListViewModel

    private val bobbyPoster = Image("image-pine-ridge", "owner-bobby", ImageKind.CAMPSITE, "campsite-pine-ridge", "/9j/4AAQSkZJRgABAQ==")
    private val bobbyProfile = Image("image-bobby-face", "owner-bobby", ImageKind.PROFILE, "owner-bobby", "/9j/4AAQSkZJRgABAg==")
    private val jillPoster = Image("image-aspen-hollow", "owner-jill", ImageKind.CAMPSITE, "campsite-aspen-hollow", "/9j/4AAQSkZJRgABAw==")
    private val trollProfile = Image("image-troll-face", "entrant-troll", ImageKind.PROFILE, "entrant-troll", "/9j/4AAQSkZJRgABBA==")
    private val goneProfile = Image("image-gone-face", "entrant-gone", ImageKind.PROFILE, "entrant-gone", "/9j/4AAQSkZJRgABBQ==")
    private val pineRidge = Campsite(
        "campsite-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 6,
        posterImageId = "image-pine-ridge",
    )
    private lateinit var campsiteRepository: CampsiteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Bobby and Jill have names; the troll's name is blank; entrant-gone's profile was deleted. */
    private fun createViewModel(
        images: List<Image> = listOf(goneProfile, trollProfile, jillPoster, bobbyProfile, bobbyPoster),
    ) {
        imageRepository = ImageRepository(MockDataSource(images))
        val userRepository = UserRepository(MockDataSource(listOf(
            User("owner-bobby", name = "Bobby Hill", owner = CampsiteOwner("owner-bobby")),
            User("owner-jill", name = "jill Tremblay", owner = CampsiteOwner("owner-jill")),
            User("entrant-troll", name = "   ", entrant = Entrant("entrant-troll")),
        )))
        campsiteRepository = CampsiteRepository(MockDataSource(listOf(pineRidge)))
        val deleter = CascadeDeleter(
            userRepository,
            campsiteRepository,
            LotteryRepository(MockDataSource(), campsiteRepository),
            ReservationRepository(MockDataSource()),
            NotificationRepository(MockDataSource()),
            RatingRepository(MockDataSource()),
            MessageRepository(MockDataSource()),
            imageRepository,
        )
        viewModel = AdminImageListViewModel(imageRepository, userRepository, deleter)
    }

    private fun TestScope.startCollecting() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    private fun rowFor(image: Image) =
        viewModel.uiState.value.images.first { it.image.id == image.id }

    @Test
    fun beforeDataArrives_isLoading() = runTest {
        createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun knownUploader_showsNameAndId() = runTest {
        createViewModel()
        startCollecting()
        advanceUntilIdle()

        val row = rowFor(jillPoster)
        assertEquals("jill Tremblay", row.uploaderName)
        assertEquals("owner-jill", row.image.uploaderId)
        assertEquals(5, viewModel.uiState.value.images.size)
    }

    @Test
    fun deletedOrBlankNamedUploader_hasNoName() = runTest {
        createViewModel()
        startCollecting()
        advanceUntilIdle()

        assertNull(rowFor(trollProfile).uploaderName)
        assertNull(rowFor(goneProfile).uploaderName)
    }

    @Test
    fun images_sortedByNamedFirstThenNameThenIdThenKind() = runTest {
        createViewModel()
        startCollecting()
        advanceUntilIdle()

        // Bobby before jill (case ignored), Bobby's poster before his profile
        // picture, then unnamed uploaders by user ID.
        assertEquals(
            listOf(bobbyPoster, bobbyProfile, jillPoster, goneProfile, trollProfile),
            viewModel.uiState.value.images.map { it.image },
        )
    }

    @Test
    fun addingOrDeletingAnImage_updatesTheList() = runTest {
        createViewModel()
        startCollecting()
        advanceUntilIdle()

        val newPoster = Image("image-birch-bay", "owner-jill", ImageKind.CAMPSITE, "campsite-birch-bay", "/9j/4AAQSkZJRgABBg==")
        imageRepository.add(newPoster)
        advanceUntilIdle()
        assertEquals(6, viewModel.uiState.value.images.size)
        assertEquals("jill Tremblay", rowFor(newPoster).uploaderName)

        imageRepository.delete(trollProfile.id)
        advanceUntilIdle()
        assertEquals(5, viewModel.uiState.value.images.size)
        assertTrue(viewModel.uiState.value.images.none { it.image.id == trollProfile.id })
    }

    @Test
    fun emptyRepository_givesEmptyListAndStopsLoading() = runTest {
        createViewModel(images = emptyList())
        startCollecting()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.images.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun tappingRemove_asksFirstAndDeletesNothing() = runTest {
        createViewModel()
        startCollecting()
        advanceUntilIdle()

        viewModel.requestRemoval(rowFor(bobbyPoster))
        advanceUntilIdle()

        assertEquals(bobbyPoster, viewModel.uiState.value.imagePendingRemoval?.image)
        assertNotNull(imageRepository.get(bobbyPoster.id))
    }

    @Test
    fun cancelling_keepsTheImage() = runTest {
        createViewModel()
        startCollecting()
        advanceUntilIdle()

        viewModel.requestRemoval(rowFor(bobbyPoster))
        viewModel.cancelRemoval()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.imagePendingRemoval)
        assertNotNull(imageRepository.get(bobbyPoster.id))
    }

    @Test
    fun confirming_deletesOnlyThatImageAndClearsTheCampsitePoster() = runTest {
        createViewModel()
        startCollecting()
        advanceUntilIdle()

        viewModel.requestRemoval(rowFor(bobbyPoster))
        viewModel.confirmRemoval()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.imagePendingRemoval)
        assertNull(imageRepository.get(bobbyPoster.id))
        assertEquals(4, viewModel.uiState.value.images.size)
        assertNull(campsiteRepository.get(pineRidge.id)?.posterImageId)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
