package com.campsitereservationapp.ui.campsite

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Entrant
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.model.ReservationStatus
import com.campsitereservationapp.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class BookedDatesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var reservationRepository: ReservationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: BookedDatesViewModel
    private val fixedToday = LocalDate.of(2026, 11, 1)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepository = CampsiteRepository(MockDataSource())
        reservationRepository = ReservationRepository(MockDataSource())
        userRepository = UserRepository(MockDataSource())

        viewModel = BookedDatesViewModel(
            reservationRepository = reservationRepository,
            userRepository = userRepository,
            campsiteRepository = campsiteRepository,
            campsiteId = "camp-pine-1",
            today = fixedToday
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun viewModel_loadsBookedDatesAndEntrantNames() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val campsite = Campsite(id = "camp-pine-1", name = "Pine Ridge")
        campsiteRepository.add(campsite)

        val user = User(id = "user-noah", name = "Noah W", entrant = Entrant("user-noah"))
        userRepository.add(user)

        val reservation = Reservation(
            id = "res-101",
            entrantId = "user-noah",
            campsiteId = "camp-pine-1",
            dates = DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5)),
            status = ReservationStatus.CONFIRMED
        )
        reservationRepository.add(reservation)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Pine Ridge", state.campsiteName)
        assertEquals(1, state.bookedDatesList.size)
        assertEquals("Noah W", state.bookedDatesList[0].entrantName)
        assertEquals(setOf(2, 3, 4, 5), state.bookedDaysOfMonth)
    }

    @Test
    fun viewModel_defaultsToEarliestUpcomingBookingMonth() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val decBooking = Reservation(
            id = "res-december",
            entrantId = "user-karl",
            campsiteId = "camp-pine-1",
            dates = DateRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3)),
            status = ReservationStatus.CONFIRMED
        )
        reservationRepository.add(decBooking)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 12), state.displayedYearMonth)
        assertEquals(setOf(1, 2, 3), state.bookedDaysOfMonth)

        // Dec 1, 2026 is a Tuesday -> dayOfWeek = TUESDAY (2) -> blanks = 2 % 7 = 2
        assertEquals(2, state.blanksBeforeFirstDay)
    }

    @Test
    fun viewModel_ignoresPastBookingsAndCancelledBookings() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val pastRes = Reservation(
            id = "res-past",
            entrantId = "user-old",
            campsiteId = "camp-pine-1",
            dates = DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10)),
            status = ReservationStatus.CONFIRMED
        )
        val cancelledRes = Reservation(
            id = "res-cancelled",
            entrantId = "user-karl",
            campsiteId = "camp-pine-1",
            dates = DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5)),
            status = ReservationStatus.CANCELLED
        )
        reservationRepository.add(pastRes)
        reservationRepository.add(cancelledRes)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.bookedDatesList.isEmpty())
    }

    @Test
    fun viewModel_handlesMonthNavigation() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val initialMonth = viewModel.uiState.value.displayedYearMonth

        viewModel.onNextMonth()
        testScheduler.advanceUntilIdle()
        assertEquals(initialMonth.plusMonths(1), viewModel.uiState.value.displayedYearMonth)

        viewModel.onPreviousMonth()
        testScheduler.advanceUntilIdle()
        assertEquals(initialMonth, viewModel.uiState.value.displayedYearMonth)
    }
}
