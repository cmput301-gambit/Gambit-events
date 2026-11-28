package com.campsitereservationapp.ui.discovery

import android.util.Log
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
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.service.CascadeDeleter
import com.campsitereservationapp.ui.profile.EntrantSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
    private lateinit var userRepository: UserRepository
    private lateinit var imageRepository: ImageRepository
    private lateinit var viewModel: EntrantSettingsViewModel
    private val tag: String = "ENTRANT-SETTINGS-VIEWMODEL-TEST"

    private lateinit var deleter: CascadeDeleter

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = UserRepository(MockDataSource(SampleData.users))
        imageRepository = ImageRepository(MockDataSource())

        deleter = CascadeDeleter(
            userRepository,
            CampsiteRepository(MockDataSource()),
            LotteryRepository(MockDataSource(), CampsiteRepository(MockDataSource())),
            ReservationRepository(MockDataSource()),
            NotificationRepository(MockDataSource()),
            RatingRepository(MockDataSource()),
            MessageRepository(MockDataSource()),
            imageRepository,
        )
        viewModel = EntrantSettingsViewModel(userRepository, "entrant-priya", deleter, imageRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun viewModel_loadsExistingUserOnEntrantSettings() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        //Log.d(tag, state.currentUser.name)
        assertEquals("Priya Shah", state.currentUser.name)
    }

    @Test
    fun updateUserDetails_persistsChanges() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()
        viewModel.updateUserDetails(newName = "New Priya Name")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("New Priya Name", state.currentUser.name)
    }
}
