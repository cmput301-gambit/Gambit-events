package com.campsitereservationapp.ui.campsite

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.SampleData
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteError
import com.campsitereservationapp.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CampsiteFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CampsiteRepository
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: CampsiteFormViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = CampsiteRepository(MockDataSource(SampleData.campsites))
        userRepository = UserRepository(MockDataSource(SampleData.users))
        viewModel = CampsiteFormViewModel(repository, userRepository, "entrant-user")
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

        val editViewModel = CampsiteFormViewModel(repository, userRepository, "owner-bobby", "campsite-edit-1")
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
}
