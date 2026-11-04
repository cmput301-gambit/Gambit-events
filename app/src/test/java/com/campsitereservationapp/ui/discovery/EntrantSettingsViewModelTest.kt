package com.campsitereservationapp.ui.discovery

import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.SampleData
import com.campsitereservationapp.data.UserRepository
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
    private lateinit var viewModel: EntrantSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = UserRepository(MockDataSource(SampleData.users))
        viewModel = EntrantSettingsViewModel(userRepository, "entrant-priya")
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
