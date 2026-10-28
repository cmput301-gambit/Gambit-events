package com.campsitereservationapp.ui.campsite

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.model.CampsiteError
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
    private lateinit var viewModel: CampsiteFormViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = CampsiteRepository()
        viewModel = CampsiteFormViewModel(repository, "owner-bobby")
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
        assertEquals("owner-bobby", savedCampsite.ownerId)
        assertEquals("Valid Campsite", savedCampsite.name)
        assertEquals(5, savedCampsite.capacity)
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
}
