package com.campsitereservationapp.ui.campsite

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.SampleData
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteError
import com.campsitereservationapp.model.Image
import com.campsitereservationapp.model.ImageKind
import com.campsitereservationapp.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

@OptIn(ExperimentalCoroutinesApi::class)
class CampsiteFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CampsiteRepository
    private lateinit var userRepository: UserRepository
    private lateinit var imageRepository: ImageRepository
    private lateinit var viewModel: CampsiteFormViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = CampsiteRepository(MockDataSource(SampleData.campsites))
        userRepository = UserRepository(MockDataSource(SampleData.users))
        imageRepository = ImageRepository(MockDataSource())
        viewModel = CampsiteFormViewModel(repository, userRepository, imageRepository, "entrant-user")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validSave_increasesRepositoryCountAndSetsSaved() = runTest {
        val initialCount = repository.getAll().size
        viewModel.onNameChange("Valid Campsite")
        viewModel.onLocationChange("Banff, AB")
        viewModel.onCapacityChange("5")

        viewModel.save()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertTrue(state.errors.isEmpty())
        assertEquals(initialCount + 1, repository.getAll().size)

        val savedCampsite = repository.getAll().last()
        assertEquals("entrant-user", savedCampsite.ownerId)
        assertEquals("Valid Campsite", savedCampsite.name)
        assertEquals(5, savedCampsite.capacity)
    }

    @Test
    fun validSave_promotesUserToOwner() = runTest {
        val userId = "entrant-user"
        val initialUser = userRepository.get(userId)
        if (initialUser != null) {
            userRepository.delete(userId)
        }
        userRepository.add(User.newEntrant(userId))
        assertFalse(userRepository.get(userId)!!.isOwner)

        viewModel.onNameChange("Valid Campsite")
        viewModel.onLocationChange("Banff, AB")
        viewModel.onCapacityChange("5")

        viewModel.save()
        testScheduler.advanceUntilIdle()

        val updatedUser = userRepository.get(userId)
        assertTrue(updatedUser!!.isOwner)
    }

    @Test
    fun invalidSave_addsNothingAndSetsErrors() = runTest {
        val initialCount = repository.getAll().size
        viewModel.onNameChange("")
        viewModel.onCapacityChange("-2")

        viewModel.save()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertTrue(state.errors.contains(CampsiteError.BLANK_NAME))
        assertTrue(state.errors.contains(CampsiteError.NON_POSITIVE_CAPACITY))
        assertEquals(initialCount, repository.getAll().size)
    }

    @Test
    fun nonNumericCapacity_setsCapacityNotANumberError() = runTest {
        val initialCount = repository.getAll().size
        viewModel.onNameChange("Good Name")
        viewModel.onCapacityChange("abc")

        viewModel.save()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertTrue(state.errors.contains(CampsiteError.CAPACITY_NOT_A_NUMBER))
        assertEquals(initialCount, repository.getAll().size)
    }

    @Test
    fun save_trimsInputs() = runTest {
        viewModel.onNameChange("  Padded Name  ")
        viewModel.onDescriptionChange("  Padded Desc  ")
        viewModel.onLocationChange("  Padded Loc  ")
        viewModel.onCapacityChange("10")

        viewModel.save()
        testScheduler.advanceUntilIdle()

        val savedCampsite = repository.getAll().last()
        assertEquals("Padded Name", savedCampsite.name)
        assertEquals("Padded Desc", savedCampsite.description)
        assertEquals("Padded Loc", savedCampsite.location)
    }

    @Test
    fun editMode_loadsExistingCampsiteDataAndUpdatesWithoutIncreasingCount() = runTest {
        val existing = Campsite(
            id = "campsite-edit-1",
            ownerId = "owner-bobby",
            name = "Original Name",
            location = "Original Loc",
            capacity = 4
        )
        repository.add(existing)
        val initialCount = repository.getAll().size

        val editViewModel = CampsiteFormViewModel(repository, userRepository, imageRepository, "owner-bobby", "campsite-edit-1")
        testScheduler.advanceUntilIdle()

        assertEquals("Original Name", editViewModel.uiState.value.name)
        assertEquals("Original Loc", editViewModel.uiState.value.location)
        assertEquals("4", editViewModel.uiState.value.capacity)

        editViewModel.onNameChange("Updated Name")
        editViewModel.save()
        testScheduler.advanceUntilIdle()

        assertTrue(editViewModel.uiState.value.isSaved)
        assertEquals(initialCount, repository.getAll().size)
        assertEquals("Updated Name", repository.get("campsite-edit-1")?.name)
    }

    // ---- Campsite posters (uploaded to ImageRepository) ----

    // The start of a real JPEG file, standing in for a shrunk photo.
    private val lakePhoto = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10, 0x4A)
    private val forestPhoto = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte(), 0x00, 0x22, 0x45)

    // Registers "Lakeview Pines" with the lake photo as its poster and returns it.
    private suspend fun TestScope.registerWithPoster(): Campsite {
        viewModel.onNameChange("Lakeview Pines")
        viewModel.onLocationChange("Canmore, AB")
        viewModel.onCapacityChange("4")
        viewModel.onPosterImagePicked(lakePhoto)
        viewModel.save()
        testScheduler.advanceUntilIdle()
        return repository.getAll().first { it.name == "Lakeview Pines" }
    }

    // Opens the edit form for a campsite and waits for it to load.
    private fun TestScope.editFormFor(campsiteId: String, ownerId: String = "entrant-user"): CampsiteFormViewModel {
        val editVm = CampsiteFormViewModel(repository, userRepository, imageRepository, ownerId, campsiteId)
        testScheduler.advanceUntilIdle()
        return editVm
    }

    @Test
    fun pickedPoster_isUploadedAndTheCampsiteKeepsItsId() = runTest {
        val saved = registerWithPoster()

        val poster = imageRepository.get(saved.posterImageId!!)
        assertNotNull(poster)
        assertEquals(ImageKind.CAMPSITE, poster!!.kind)
        assertEquals(saved.id, poster.targetId)
        assertEquals("entrant-user", poster.uploaderId)
        assertEquals(Image.encode(lakePhoto), poster.data)
    }

    @Test
    fun noPoster_uploadsNothing() = runTest {
        viewModel.onNameChange("Bare Flats")
        viewModel.onLocationChange("Hinton, AB")
        viewModel.onCapacityChange("2")
        viewModel.save()
        testScheduler.advanceUntilIdle()

        assertNull(repository.getAll().first { it.name == "Bare Flats" }.posterImageId)
        assertTrue(imageRepository.getAll().isEmpty())
    }

    @Test
    fun editing_showsTheSavedPoster() = runTest {
        val saved = registerWithPoster()

        val editVm = editFormFor(saved.id)

        assertEquals(Image.encode(lakePhoto), editVm.uiState.value.posterImage)
    }

    @Test
    fun editingWithoutTouchingThePoster_keepsItAndUploadsNothingNew() = runTest {
        val saved = registerWithPoster()

        val editVm = editFormFor(saved.id)
        editVm.onNameChange("Lakeview Pines North")
        editVm.save()
        testScheduler.advanceUntilIdle()

        assertEquals(saved.posterImageId, repository.get(saved.id)!!.posterImageId)
        assertEquals(1, imageRepository.getAll().size)
    }

    @Test
    fun replacingThePoster_uploadsTheNewOneAndDeletesTheOld() = runTest {
        val saved = registerWithPoster()

        val editVm = editFormFor(saved.id)
        editVm.onPosterImagePicked(forestPhoto)
        editVm.save()
        testScheduler.advanceUntilIdle()

        val newId = repository.get(saved.id)!!.posterImageId!!
        assertTrue(newId != saved.posterImageId)
        assertNull(imageRepository.get(saved.posterImageId!!))
        assertEquals(Image.encode(forestPhoto), imageRepository.get(newId)!!.data)
        assertEquals(1, imageRepository.getAll().size)
    }

    @Test
    fun removingThePoster_clearsItAndDeletesTheImage() = runTest {
        val saved = registerWithPoster()

        val editVm = editFormFor(saved.id)
        editVm.onPosterImageRemoved()
        editVm.save()
        testScheduler.advanceUntilIdle()

        assertNull(repository.get(saved.id)!!.posterImageId)
        assertTrue(imageRepository.getAll().isEmpty())
    }

    @Test
    fun editingAnOldCampsiteWithAPhoneOnlyLink_showsNoPosterAndStillSaves() = runTest {
        val old = Campsite(
            id = "campsite-old-link", ownerId = "owner-bobby", name = "Old Link Camp",
            location = "Jasper, AB", capacity = 3,
            posterImageId = "content://media/picker/0/com.android.providers.media.photopicker/media/1000000033",
        )
        repository.add(old)

        val editVm = editFormFor(old.id, ownerId = "owner-bobby")
        assertNull(editVm.uiState.value.posterImage)
        assertFalse(editVm.uiState.value.loadError)

        editVm.onPosterImagePicked(lakePhoto)
        editVm.save()
        testScheduler.advanceUntilIdle()

        assertTrue(editVm.uiState.value.isSaved)
        assertNotNull(imageRepository.get(repository.get(old.id)!!.posterImageId!!))
    }

    @Test
    fun posterTooLargeToStore_isASaveErrorAndNothingIsSaved() = runTest {
        viewModel.onNameChange("Huge Photo Camp")
        viewModel.onLocationChange("Banff, AB")
        viewModel.onCapacityChange("4")
        viewModel.onPosterImagePicked(ByteArray(Image.MAX_DATA_LENGTH)) // about 1.2 MB once encoded
        val before = repository.getAll().size

        viewModel.save()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveError)
        assertEquals(before, repository.getAll().size)
        assertTrue(imageRepository.getAll().isEmpty())
    }
}
