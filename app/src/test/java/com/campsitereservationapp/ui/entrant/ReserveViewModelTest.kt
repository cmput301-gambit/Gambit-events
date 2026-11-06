package com.campsitereservationapp.ui.entrant

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.model.ReservationStatus
import com.campsitereservationapp.model.WaitingList
import com.campsitereservationapp.ui.discovery.ReserveViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
 * Unit tests for [ReserveViewModel].
 *
 * **Purpose:** checks that only winners of a drawn lottery can reserve, that
 * free dates are worked out correctly, and that missing, out-of-range or
 * already-taken dates are refused (US 01.04.01, 01.05.01).
 *
 * **Design rationale:** repositories backed by [MockDataSource], so nothing
 * touches Firestore. Dates are passed in the way the date range picker
 * hands them over.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReserveViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var lotteryRepository: LotteryRepository
    private lateinit var reservationRepository: ReservationRepository

    private fun december(start: Int, finish: Int) =
        DateRange(LocalDate.of(2026, 12, start), LocalDate.of(2026, 12, finish))

    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 12, dayOfMonth)

    private val pineRidge = Campsite(
        id = "campsite-pine-ridge",
        ownerId = "owner-bobby",
        name = "Pine Ridge",
        availability = december(1, 10),
    )

    // Priya won. Ali entered and lost.
    private val closedLottery = Lottery(
        id = "lottery-pine-ridge",
        ownerId = "owner-bobby",
        campsiteIds = listOf("campsite-pine-ridge"),
        registrationPeriod = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5)),
        reservationPeriod = DateRange(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 20)),
        stage = LotteryStatus.CLOSED,
        waitingList = WaitingList(
            entrantList = listOf("entrant-priya", "entrant-ali"),
            entrantWinners = listOf("entrant-priya"),
            entrantLosers = listOf("entrant-ali"),
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepository = CampsiteRepository(MockDataSource(listOf(pineRidge)))
        lotteryRepository = LotteryRepository(MockDataSource(listOf(closedLottery)), campsiteRepository)
        reservationRepository = ReservationRepository(MockDataSource())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(userId: String) =
        ReserveViewModel(lotteryRepository, campsiteRepository, reservationRepository, userId)

    private fun aliBooks(dates: DateRange) = Reservation(
        id = "reservation-ali",
        entrantId = "entrant-ali",
        campsiteId = "campsite-pine-ridge",
        lotteryId = "lottery-pine-ridge",
        dates = dates,
    )

    @Test
    fun freeRanges_noBookings_isTheWholeAvailability() {
        assertEquals(listOf(december(1, 10)), ReserveViewModel.freeRanges(december(1, 10), emptyList()))
    }

    @Test
    fun freeRanges_bookingInTheMiddle_splitsIntoTwo() {
        val free = ReserveViewModel.freeRanges(december(1, 10), listOf(december(4, 5)))

        assertEquals(listOf(december(1, 3), december(6, 10)), free)
    }

    @Test
    fun freeRanges_everythingBooked_isEmpty() {
        assertTrue(ReserveViewModel.freeRanges(december(1, 10), listOf(december(1, 10))).isEmpty())
    }

    @Test
    fun winner_seesTheFreeRanges() = runTest {
        reservationRepository.reserve(aliBooks(december(4, 5)))
        val viewModel = viewModelFor("entrant-priya")

        viewModel.load("lottery-pine-ridge")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Pine Ridge", state.campsiteName)
        assertEquals(listOf(december(1, 3), december(6, 10)), state.freeRanges)
        assertTrue(state.canReserve)
    }

    @Test
    fun loser_isBlocked() = runTest {
        val viewModel = viewModelFor("entrant-ali")

        viewModel.load("lottery-pine-ridge")
        advanceUntilIdle()

        assertEquals("Only lottery winners can reserve dates.", viewModel.uiState.value.blockedReason)
        assertFalse(viewModel.uiState.value.canReserve)
    }

    @Test
    fun lotteryNotDrawnYet_isBlocked() = runTest {
        lotteryRepository = LotteryRepository(
            MockDataSource(listOf(closedLottery.copy(stage = LotteryStatus.OPEN))), campsiteRepository)
        val viewModel = viewModelFor("entrant-priya")

        viewModel.load("lottery-pine-ridge")
        advanceUntilIdle()

        assertEquals("This lottery hasn't been drawn yet.", viewModel.uiState.value.blockedReason)
    }

    @Test
    fun winner_reservesFreeDates_getsConfirmation() = runTest {
        val viewModel = viewModelFor("entrant-priya")
        viewModel.load("lottery-pine-ridge")
        advanceUntilIdle()

        viewModel.onDatesPicked(day(2), day(4))
        viewModel.reserve()
        advanceUntilIdle()

        val confirmation = viewModel.uiState.value.confirmation
        assertNotNull(confirmation)
        assertEquals("entrant-priya", confirmation!!.entrantId)
        assertEquals(december(2, 4), confirmation.dates)
        assertEquals(ReservationStatus.CONFIRMED, confirmation.status)
        assertEquals(1, reservationRepository.getAll().size)
    }

    @Test
    fun singleDayPicked_reservesThatOneDay() = runTest {
        val viewModel = viewModelFor("entrant-priya")
        viewModel.load("lottery-pine-ridge")
        advanceUntilIdle()

        viewModel.onDatesPicked(day(7), null)
        viewModel.reserve()
        advanceUntilIdle()

        assertEquals(december(7, 7), viewModel.uiState.value.confirmation?.dates)
    }

    @Test
    fun noDatesPicked_isRefused() = runTest {
        val viewModel = viewModelFor("entrant-priya")
        viewModel.load("lottery-pine-ridge")
        advanceUntilIdle()

        viewModel.reserve()
        advanceUntilIdle()

        assertEquals("Choose your check-in and check-out dates first.", viewModel.uiState.value.error)
        assertTrue(reservationRepository.getAll().isEmpty())
    }

    @Test
    fun datesOutsideTheFreeRanges_areRefused() = runTest {
        val viewModel = viewModelFor("entrant-priya")
        viewModel.load("lottery-pine-ridge")
        advanceUntilIdle()

        viewModel.onDatesPicked(day(8), day(12))
        viewModel.reserve()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.confirmation)
        assertTrue(reservationRepository.getAll().isEmpty())
    }

    @Test
    fun checkOutBeforeCheckIn_isRefused() = runTest {
        val viewModel = viewModelFor("entrant-priya")
        viewModel.load("lottery-pine-ridge")
        advanceUntilIdle()

        viewModel.onDatesPicked(day(5), day(3))
        viewModel.reserve()
        advanceUntilIdle()

        assertEquals("Check-out can't be before check-in.", viewModel.uiState.value.error)
        assertTrue(reservationRepository.getAll().isEmpty())
    }

    @Test
    fun datesTakenAfterLoading_areRefused() = runTest {
        val viewModel = viewModelFor("entrant-priya")
        viewModel.load("lottery-pine-ridge")
        advanceUntilIdle()

        // Ali books these dates after Priya's screen loaded.
        reservationRepository.reserve(aliBooks(december(2, 4)))
        viewModel.onDatesPicked(day(3), day(5))
        viewModel.reserve()
        advanceUntilIdle()

        assertEquals("Those dates were just taken. Pick other dates.", viewModel.uiState.value.error)
        assertEquals(1, reservationRepository.getAll().size)
    }

    @Test
    fun winnerWhoAlreadyReserved_seesTheirReservation() = runTest {
        val first = viewModelFor("entrant-priya")
        first.load("lottery-pine-ridge")
        advanceUntilIdle()
        first.onDatesPicked(day(2), day(4))
        first.reserve()
        advanceUntilIdle()

        val second = viewModelFor("entrant-priya")
        second.load("lottery-pine-ridge")
        advanceUntilIdle()

        assertEquals(december(2, 4), second.uiState.value.confirmation?.dates)
        assertFalse(second.uiState.value.canReserve)
    }
}