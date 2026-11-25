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

@OptIn(ExperimentalCoroutinesApi::class)
class BookedDatesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var reservationRepository: ReservationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: BookedDatesViewModel

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
            campsiteId = "camp-pine-1"
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
            dates = DateRange(LocalDate.now(), LocalDate.now().plusDays(2)),
            status = ReservationStatus.CONFIRMED
        )
        reservationRepository.add(reservation)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Pine Ridge", state.campsiteName)
        assertEquals(1, state.bookedDatesList.size)
        assertEquals("Noah W", state.bookedDatesList[0].entrantName)
    }

    @Test
    fun viewModel_ignoresCancelledAndOtherCampsiteReservations() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val cancelledRes = Reservation(
            id = "res-cancelled",
            entrantId = "user-karl",
            campsiteId = "camp-pine-1",
            dates = DateRange(LocalDate.now(), LocalDate.now().plusDays(2)),
            status = ReservationStatus.CANCELLED
        )
        val otherCampRes = Reservation(
            id = "res-other",
            entrantId = "user-vince",
            campsiteId = "camp-lakeside-2",
            dates = DateRange(LocalDate.now(), LocalDate.now().plusDays(2)),
            status = ReservationStatus.CONFIRMED
        )
        reservationRepository.add(cancelledRes)
        reservationRepository.add(otherCampRes)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.bookedDatesList.isEmpty())
    }
}
