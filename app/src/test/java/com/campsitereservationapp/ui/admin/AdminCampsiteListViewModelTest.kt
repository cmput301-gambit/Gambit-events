package com.campsitereservationapp.ui.admin

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
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
import java.time.LocalDate

/**
 * Unit tests for [AdminCampsiteListViewModel].
 *
 * **Purpose:** checks the admin sees every campsite, that Remove asks for
 * confirmation first, that cancelling keeps the campsite, and that confirming
 * deletes the campsite and cleans up its lotteries (US 03.04.01, 03.01.01).
 *
 * **Design rationale:** repositories backed by [MockDataSource], so nothing
 * touches Firestore.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminCampsiteListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var lotteryRepository: LotteryRepository
    private lateinit var viewModel: AdminCampsiteListViewModel

    private val pineRidge = Campsite("campsite-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge")
    private val aspenHollow = Campsite("campsite-aspen-hollow", ownerId = "owner-jill", name = "Aspen Hollow")

    private val registrationPeriod = DateRange(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5))
    private val reservationPeriod = DateRange(LocalDate.now().plusDays(6), LocalDate.now().plusDays(8))

    // A lottery for Pine Ridge only, and one shared by both campsites.
    private val pineRidgeLottery = Lottery(
        "lottery-pine-ridge",
        ownerId = "owner-bobby",
        campsiteIds = mutableListOf("campsite-pine-ridge"),
        registrationPeriod = registrationPeriod,
        reservationPeriod = reservationPeriod,
    )
    private val sharedLottery = Lottery(
        "lottery-shared",
        ownerId = "owner-bobby",
        campsiteIds = mutableListOf("campsite-pine-ridge", "campsite-aspen-hollow"),
        registrationPeriod = registrationPeriod,
        reservationPeriod = reservationPeriod,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepository = CampsiteRepository(MockDataSource(listOf(pineRidge, aspenHollow)))
        lotteryRepository = LotteryRepository(
            MockDataSource(listOf(pineRidgeLottery, sharedLottery)), campsiteRepository)
        viewModel = AdminCampsiteListViewModel(campsiteRepository, lotteryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.startCollecting() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    @Test
    fun beforeDataArrives_isLoading() = runTest {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun adminSeesEveryCampsite() = runTest {
        startCollecting()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.campsites.size)
    }

    @Test
    fun tappingRemove_asksFirstAndDeletesNothing() = runTest {
        startCollecting()
        advanceUntilIdle()

        viewModel.requestRemoval(pineRidge)
        advanceUntilIdle()

        assertEquals(pineRidge, viewModel.uiState.value.campsitePendingRemoval)
        assertNotNull(campsiteRepository.get("campsite-pine-ridge"))
    }

    @Test
    fun cancelling_keepsTheCampsite() = runTest {
        startCollecting()
        advanceUntilIdle()

        viewModel.requestRemoval(pineRidge)
        viewModel.cancelRemoval()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.campsitePendingRemoval)
        assertNotNull(campsiteRepository.get("campsite-pine-ridge"))
        assertEquals(2, viewModel.uiState.value.campsites.size)
    }

    @Test
    fun confirming_deletesTheCampsite() = runTest {
        startCollecting()
        advanceUntilIdle()

        viewModel.requestRemoval(pineRidge)
        viewModel.confirmRemoval()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.campsitePendingRemoval)
        assertNull(campsiteRepository.get("campsite-pine-ridge"))
        assertEquals(listOf(aspenHollow), viewModel.uiState.value.campsites)
    }

    @Test
    fun confirming_cleansUpItsLotteries() = runTest {
        startCollecting()
        advanceUntilIdle()

        viewModel.requestRemoval(pineRidge)
        viewModel.confirmRemoval()
        advanceUntilIdle()

        // Pine Ridge's own lottery is gone, and the shared one just loses Pine Ridge.
        assertNull(lotteryRepository.get("lottery-pine-ridge"))
        assertEquals(
            listOf("campsite-aspen-hollow"),
            lotteryRepository.get("lottery-shared")!!.campsiteIds,
        )
    }

    @Test
    fun confirmingWithNothingPending_doesNothing() = runTest {
        startCollecting()
        advanceUntilIdle()

        viewModel.confirmRemoval()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.campsites.size)
    }
}