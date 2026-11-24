package com.campsitereservationapp.ui.campsite

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.User
import com.campsitereservationapp.model.WaitingList
import com.campsitereservationapp.ui.owner.OwnerWaitlistViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [com.campsitereservationapp.ui.owner.OwnerWaitlistViewModel].
 *
 * **Purpose:** checks an owner sees only their own lotteries, with the
 * campsite name and the names of everyone on each waitlist, and that the
 * screen shows loading until data arrives (US 02.09.01).
 *
 * **Design rationale:** repositories backed by [MockDataSource], so nothing
 * touches Firestore.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerWaitlistViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var lotteryRepository: LotteryRepository
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var userRepository: UserRepository

    private val registrationPeriod = DateRange(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5))
    private val reservationPeriod = DateRange(LocalDate.now().plusDays(6), LocalDate.now().plusDays(8))

    private val bobbysLottery = Lottery(
        "lottery-pine-ridge",
        ownerId = "owner-bobby",
        campsiteIds = mutableListOf("campsite-pine-ridge"),
        registrationPeriod = registrationPeriod,
        reservationPeriod = reservationPeriod,
        stage = LotteryStatus.OPEN,
        waitingList = WaitingList(mutableListOf("entrant-priya", "entrant-ali")),
    )
    private val jillsLottery = Lottery(
        "lottery-aspen-hollow",
        ownerId = "owner-jill",
        campsiteIds = mutableListOf("campsite-aspen-hollow"),
        registrationPeriod = registrationPeriod,
        reservationPeriod = reservationPeriod,
        stage = LotteryStatus.OPEN,
        waitingList = WaitingList(mutableListOf("entrant-ben")),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepository = CampsiteRepository(MockDataSource(listOf(
            Campsite("campsite-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge"),
            Campsite("campsite-aspen-hollow", ownerId = "owner-jill", name = "Aspen Hollow"),
        )))
        userRepository = UserRepository(MockDataSource(listOf(
            User(id = "entrant-priya", name = "Priya Shah"),
            User(id = "entrant-ali", name = "Ali Khan"),
        )))
        lotteryRepository = LotteryRepository(
            MockDataSource(listOf(bobbysLottery, jillsLottery)), campsiteRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(ownerId: String) =
        OwnerWaitlistViewModel(lotteryRepository, campsiteRepository, userRepository, ownerId)

    private fun TestScope.startCollecting(viewModel: OwnerWaitlistViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    @Test
    fun ownerSeesOnlyTheirOwnLotteries() = runTest {
        val viewModel = viewModelFor("owner-bobby")
        startCollecting(viewModel)
        advanceUntilIdle()

        val lotteries = viewModel.uiState.value.lotteries
        assertEquals(1, lotteries.size)
        assertEquals("lottery-pine-ridge", lotteries[0].lotteryId)
        assertEquals("Pine Ridge", lotteries[0].campsiteNames)
    }

    @Test
    fun waitlistShowsEntrantNames() = runTest {
        val viewModel = viewModelFor("owner-bobby")
        startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(listOf("Priya Shah", "Ali Khan"), viewModel.uiState.value.lotteries[0].entrantNames)
    }

    @Test
    fun entrantWithNoSavedName_showsTheirId() = runTest {
        val viewModel = viewModelFor("owner-jill")
        startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(listOf("entrant-ben"), viewModel.uiState.value.lotteries[0].entrantNames)
    }

    @Test
    fun ownerWithNoLotteries_seesEmptyList() = runTest {
        val viewModel = viewModelFor("owner-sam")
        startCollecting(viewModel)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lotteries.isEmpty())
    }

    @Test
    fun someoneJoining_showsUpOnTheWaitlist() = runTest {
        val viewModel = viewModelFor("owner-bobby")
        startCollecting(viewModel)
        advanceUntilIdle()

        lotteryRepository.update(bobbysLottery.copy(
            waitingList = WaitingList(mutableListOf("entrant-priya", "entrant-ali", "entrant-ben"))))
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.lotteries[0].entrantNames.size)
    }

    @Test
    fun beforeDataArrives_isLoading() = runTest {
        val viewModel = viewModelFor("owner-bobby")

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun afterDataArrives_stopsLoading() = runTest {
        val viewModel = viewModelFor("owner-sam")
        startCollecting(viewModel)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.lotteries.isEmpty())
    }
}