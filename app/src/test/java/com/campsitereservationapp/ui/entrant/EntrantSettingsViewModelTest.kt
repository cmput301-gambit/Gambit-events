package com.campsitereservationapp.ui.entrant

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MessageRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.data.RatingRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.SampleData
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Image
import com.campsitereservationapp.model.ImageKind
import com.campsitereservationapp.model.User
import com.campsitereservationapp.service.CascadeDeleter
import com.campsitereservationapp.ui.profile.EntrantSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class EntrantSettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: UserRepository
    private lateinit var imageRepository: ImageRepository
    private lateinit var viewModel: EntrantSettingsViewModel
    private val userId: String = "entrant-SETTINGSVIEWMODELTEST"
    private val user: User = User(
        id = userId,
        name = "bad Name",
        email = "bad email",
        phone = "bad phone",
    )
    private lateinit var deleter: CascadeDeleter

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = UserRepository(MockDataSource(SampleData.users))
        imageRepository = ImageRepository(MockDataSource())
        deleter = CascadeDeleter(
                repository,
        CampsiteRepository(MockDataSource()),
        LotteryRepository(MockDataSource(), CampsiteRepository(MockDataSource())),
        ReservationRepository(MockDataSource()),
        NotificationRepository(MockDataSource()),
        RatingRepository(MockDataSource()),
        MessageRepository(MockDataSource()),
        imageRepository,
        )
        viewModel = EntrantSettingsViewModel(repository, userId, deleter, imageRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    //NOTE: Cannot figure out why the user is instantly added to the user repository, which caused repository.add(user) to fail every time.
    @Test
    fun valid_CanToggleNotifications() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            try {
                repository.add(user)
            } catch (e: Exception) {
                println("add failed: $e")
            }

            viewModel.uiState.collect {  }
        }
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.currentUser.notificationsEnabled)

        viewModel.toggleReceiveNotifications()

        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.currentUser.notificationsEnabled)

    }
    @Test
    fun valid_CanUpdateUserDetails() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            try {
                repository.add(user)
            } catch (e: Exception) {
                println("add failed: $e")
            }

            viewModel.uiState.collect {  }
        }
        advanceUntilIdle()
        viewModel.updateUserDetails(
            newName = "name1",
            newPhone = "phone1",
            newEmail = "email1"
        )
        advanceUntilIdle()
        assertEquals("name1", viewModel.uiState.value.currentUser.name)
        assertEquals("phone1", viewModel.uiState.value.currentUser.phone)
        assertEquals("email1", viewModel.uiState.value.currentUser.email)

    }

    @Test
    fun canSetProfilePictureFromNull() = runTest{
        val newUser = User(id=repository.newId())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            try{
                repository.add(newUser)
            } catch (e: Exception){
                println("Failed to add new user")
            }

            viewModel.uiState.collect {  }
        }
        val newImg = Image(id = "id", kind = ImageKind.PROFILE, data = "123")
        val oldPfp = viewModel.uiState.value.currentProfileImage
        advanceUntilIdle()
        viewModel.onProfilePicturePicked(newImg.bytes()?: ByteArray(0))
        advanceUntilIdle()
        viewModel.updateUserDetails()
        advanceUntilIdle()
        println("Old = ${oldPfp}, New = ${viewModel.uiState.value.currentProfileImage}")
        println("Value in repo = ${imageRepository.get("id")}")
        assert(imageRepository.get(viewModel.uiState.value.currentUser.profileImageId?:"") != null)
    }

    @Test
    fun canSetProfilePictureToNull() = runTest{
        val newUser = User(id=repository.newId())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            try{
                repository.add(newUser)
            } catch (e: Exception){
                println("Failed to add new user")
            }

            viewModel.uiState.collect {  }
        }
        val newImg = Image(id = "id", kind = ImageKind.PROFILE, data = "123")
        advanceUntilIdle()
        viewModel.onProfilePicturePicked(newImg.bytes()?: ByteArray(0))
        advanceUntilIdle()
        viewModel.updateUserDetails()
        advanceUntilIdle()
        viewModel.onProfilePictureRemoved()
        advanceUntilIdle()
        viewModel.updateUserDetails()
        advanceUntilIdle()
        assertEquals(viewModel.uiState.value.currentProfileImage, null)
    }

    @Test
    fun canSetProfileFromPictureToPicture() = runTest{
        val newUser = User(id=repository.newId())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            try{
                repository.add(newUser)
            } catch (e: Exception){
                println("Failed to add new user")
            }

            viewModel.uiState.collect {  }
        }
        val img1 = Image(id = "id", kind = ImageKind.PROFILE, data = "123")
        val img2 = Image(id = "id", kind = ImageKind.PROFILE, data = "123")
        advanceUntilIdle()
        viewModel.onProfilePicturePicked(img1.bytes()?: ByteArray(0))
        advanceUntilIdle()
        viewModel.updateUserDetails()
        advanceUntilIdle()
        viewModel.onProfilePicturePicked(img2.bytes()?: ByteArray(0))
        advanceUntilIdle()
        viewModel.updateUserDetails()
        advanceUntilIdle()
        assertEquals(Image.encode(img2.bytes()?: ByteArray(0)), viewModel.uiState.value.currentProfileImage)
    }
}