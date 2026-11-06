package com.campsitereservationapp.ui.entrant

import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.SampleData
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.User
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
    private lateinit var viewModel: EntrantSettingsViewModel
    private val userId: String = "entrant-SETTINGSVIEWMODELTEST"
    private val user: User = User(
        id = userId,
        name = "bad Name",
        email = "bad email",
        phone = "bad phone",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = UserRepository(MockDataSource(SampleData.users))
        viewModel = EntrantSettingsViewModel(repository, userId)
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
}