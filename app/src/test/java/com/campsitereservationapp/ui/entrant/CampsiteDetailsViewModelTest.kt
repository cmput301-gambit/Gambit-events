package com.campsitereservationapp.ui.entrant

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.SampleData
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.ui.campsite.CampsiteDetailsMainButtonState
import com.campsitereservationapp.ui.campsite.CampsiteDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CampsiteDetailsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CampsiteRepository
    private lateinit var userRepository: UserRepository

    private lateinit var lotteryRepository: LotteryRepository
    private lateinit var viewModel: CampsiteDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = CampsiteRepository(MockDataSource(SampleData.campsites))
        userRepository = UserRepository(MockDataSource(SampleData.users))
        lotteryRepository = LotteryRepository(MockDataSource(SampleData.lotteries), repository)
        viewModel = CampsiteDetailsViewModel(lotteryRepository, repository, "entrant-user")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun valid_CanJoinCampsiteLottery() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {  }
        }
        val campId = "campsite-lakeside-loop"
        viewModel.updateViewedCampSiteId(campId)

        println(repository.get(campId))
        advanceUntilIdle()
        println("valid_CanJoinCampsiteLottery: " + "Attempting to join campsite ${viewModel.uiState.value.viewedCampsite}")
        println("valid_CanJoinCampsiteLottery: " + "Currently viewed campsite lottery waitlist: ${lotteryRepository.listLotteries(campsiteId = "campsite-lakeside-loop").first()[0].waitingList.entrantList}")
        assertEquals(viewModel.uiState.value.viewedCampsite.id, campId)
        assertEquals(viewModel.uiState.value.mainButtonState, CampsiteDetailsMainButtonState.JOIN)
        viewModel.updateButtonState()

        advanceUntilIdle()

        assertEquals(CampsiteDetailsMainButtonState.LEAVE, viewModel.uiState.value.mainButtonState)
        //"campsite-aspen-hollow"
    }
    @Test
    fun valid_CanLeaveCampsiteLottery() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {  }
        }
        val campId = "campsite-aspen-hollow"
        viewModel.updateViewedCampSiteId(campId)

        println(repository.get(campId))
        advanceUntilIdle()
        println("valid_CanLeaveCampsiteLottery: " + "Attempting to leave campsite ${viewModel.uiState.value.viewedCampsite}")
        println("valid_CanLeaveCampsiteLottery: " + "Currently viewed campsite lottery waitlist: ${lotteryRepository.listLotteries(campsiteId = "campsite-lakeside-loop").first()[0].waitingList.entrantList}")
        assertEquals(viewModel.uiState.value.viewedCampsite.id, campId)
        assertEquals(viewModel.uiState.value.mainButtonState, CampsiteDetailsMainButtonState.LEAVE)
        viewModel.updateButtonState()

        advanceUntilIdle()

        assertEquals(CampsiteDetailsMainButtonState.JOIN, viewModel.uiState.value.mainButtonState)
    }
    @Test
    fun valid_CanJoinAndLeaveCampsiteLottery() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {  }
        }
        val campId = "camp-a"
        val camp: Campsite = Campsite(
            id = campId,
            availability = DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)),
        )
        val lottery: Lottery = Lottery(
            id = "lottery-c",
            campsiteIds = mutableListOf(campId),
            registrationPeriod = DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)),
            reservationPeriod = DateRange(LocalDate.now().plusDays(2), LocalDate.now().plusDays(3)),
            stage = LotteryStatus.OPEN
        )

        lotteryRepository.add(lottery)
        repository.add(camp)

        viewModel.updateViewedCampSiteId(campId)

        println(repository.get(campId))

        advanceUntilIdle()

        println("valid_CanJoinAndLeaveCampsiteLottery: " + "Attempting to leave campsite ${viewModel.uiState.value.viewedCampsite}")
        println("valid_CanJoinAndLeaveCampsiteLottery: " + "Currently viewed campsite lottery waitlist: ${lotteryRepository.listLotteries(campsiteId = "campsite-lakeside-loop").first()[0].waitingList.entrantList}")
        assertEquals(campId, viewModel.uiState.value.viewedCampsite.id)
        assertEquals(CampsiteDetailsMainButtonState.JOIN, viewModel.uiState.value.mainButtonState)
        viewModel.updateButtonState()

        advanceUntilIdle()

        assertEquals(CampsiteDetailsMainButtonState.LEAVE, viewModel.uiState.value.mainButtonState)
        val updatedLottery = lotteryRepository.get("lottery-c")
        assert(updatedLottery?.waitingList?.entrantList?.contains("entrant-user") == true)

        viewModel.updateButtonState()

        advanceUntilIdle()

        assertEquals(CampsiteDetailsMainButtonState.JOIN, viewModel.uiState.value.mainButtonState)
        val leftLottery = lotteryRepository.get("lottery-c")
        assert(leftLottery?.waitingList?.entrantList?.contains("entrant-user") == false)

    }
}