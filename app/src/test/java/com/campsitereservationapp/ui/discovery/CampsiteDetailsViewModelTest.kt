package com.campsitereservationapp.ui.discovery

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.ui.campsite.CampsiteDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CampsiteDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var lotteryRepository: LotteryRepository
    private lateinit var viewModel: CampsiteDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepository = CampsiteRepository(MockDataSource())
        lotteryRepository = LotteryRepository(MockDataSource(), campsiteRepository)
        viewModel = CampsiteDetailsViewModel(lotteryRepository, campsiteRepository, "owner-bobby")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun viewModel_loadsCampsiteDetailsById() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val testCampsite = Campsite(
            id = "test-camp-100",
            ownerId = "owner-bobby",
            name = "Mountain View",
            location = "Banff, AB",
            capacity = 10,
            tags = listOf("hiking", "views"),
            latitude = 51.1784,
            longitude = -115.5708,
            availability = DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10))
        )
        campsiteRepository.add(testCampsite)

        viewModel.updateViewedCampSiteId("test-camp-100")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Mountain View", state.viewedCampsite.name)
        assertEquals("Banff, AB", state.viewedCampsite.location)
        assertEquals(10, state.viewedCampsite.capacity)
        assertEquals(listOf("hiking", "views"), state.viewedCampsite.tags)
        assertEquals(51.1784, state.viewedCampsite.latitude!!, 0.0001)
        assertEquals(-115.5708, state.viewedCampsite.longitude!!, 0.0001)
        assertNotNull(state.viewedCampsite.availability)
    }
}
