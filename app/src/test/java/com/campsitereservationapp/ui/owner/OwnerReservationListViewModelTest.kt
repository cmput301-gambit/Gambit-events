package com.campsitereservationapp.ui.owner

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [OwnerReservationListViewModel].
 *
 * **Purpose:** verifies that the owner reservation list view model correctly filters
 * reservations by owner campsite IDs, resolves campsite and entrant names, filters out
 * past/cancelled reservations, handles guest name fallbacks, tracks loading state, and supports
 * live updates when reservations are cancelled.
 *
 * **Design rationale:** uses a StandardTestDispatcher and Coroutines test rules with mock
 * repositories seeded with test campsites, users, and reservations.
 *
 * **Outstanding issues:** none.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerReservationListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var reservationRepository: ReservationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: OwnerReservationListViewModel
    private val fixedToday = LocalDate.of(2026, 11, 1)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepository = CampsiteRepository(MockDataSource())
        reservationRepository = ReservationRepository(MockDataSource())
        userRepository = UserRepository(MockDataSource())

        viewModel = OwnerReservationListViewModel(
            reservationRepository = reservationRepository,
            campsiteRepository = campsiteRepository,
            userRepository = userRepository,
            currentUserId = "owner-bobby",
            today = fixedToday,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun viewModel_filtersReservationsOnlyForOwnerCampsitesAndResolvesNames() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val bobbyCamp = Campsite(id = "camp-bobby", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 5)
        val jillCamp = Campsite(id = "camp-jill", ownerId = "owner-jill", name = "Aspen Hollow", capacity = 5)
        campsiteRepository.add(bobbyCamp)
        campsiteRepository.add(jillCamp)

        val guestUser = User(id = "guest-noah", name = "Noah W", entrant = Entrant("guest-noah"))
        userRepository.add(guestUser)

        val bobbyReservation = Reservation(
            id = "res-bobby",
            entrantId = "guest-noah",
            campsiteId = "camp-bobby",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5)),
            status = ReservationStatus.CONFIRMED
        )
        val jillReservation = Reservation(
            id = "res-jill",
            entrantId = "guest-noah",
            campsiteId = "camp-jill",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5)),
            status = ReservationStatus.CONFIRMED
        )
        reservationRepository.add(bobbyReservation)
        reservationRepository.add(jillReservation)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        assertEquals("res-bobby", state.items[0].reservation.id)
        assertEquals("Pine Ridge", state.items[0].campsiteName)
        assertEquals("Noah W", state.items[0].guestName)
    }

    @Test
    fun viewModel_filtersOutInactivePastAndKeepsBoundaryReservations() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val bobbyCamp = Campsite(id = "camp-bobby", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 5)
        campsiteRepository.add(bobbyCamp)

        val guestUser = User(id = "guest-noah", name = "Noah W", entrant = Entrant("guest-noah"))
        userRepository.add(guestUser)

        // Cancelled reservation
        val cancelledRes = Reservation(
            id = "res-cancelled",
            entrantId = "guest-noah",
            campsiteId = "camp-bobby",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5)),
            status = ReservationStatus.CANCELLED
        )
        // Past reservation (finish before fixedToday)
        val pastRes = Reservation(
            id = "res-past",
            entrantId = "guest-noah",
            campsiteId = "camp-bobby",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 31)),
            status = ReservationStatus.CONFIRMED
        )
        // Boundary reservation ending exactly on fixedToday (should stay)
        val boundaryRes = Reservation(
            id = "res-boundary",
            entrantId = "guest-noah",
            campsiteId = "camp-bobby",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 10, 28), fixedToday),
            status = ReservationStatus.CONFIRMED
        )

        reservationRepository.add(cancelledRes)
        reservationRepository.add(pastRes)
        reservationRepository.add(boundaryRes)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        assertEquals("res-boundary", state.items[0].reservation.id)
    }

    @Test
    fun viewModel_guestFallback_usesGuestWhenUserMissing() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val bobbyCamp = Campsite(id = "camp-bobby", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 5)
        campsiteRepository.add(bobbyCamp)

        // Reservation with unknown entrantId not in userRepository
        val res = Reservation(
            id = "res-unknown",
            entrantId = "unknown-entrant-id",
            campsiteId = "camp-bobby",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5)),
            status = ReservationStatus.CONFIRMED
        )
        reservationRepository.add(res)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        assertEquals("Guest", state.items[0].guestName)
    }

    @Test
    fun viewModel_loadingState_isTrueBeforeCollectAndFalseAfter() = runTest {
        assertTrue(viewModel.uiState.value.isLoading)

        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun viewModel_liveUpdates_removesReservationWhenCancelled() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val bobbyCamp = Campsite(id = "camp-bobby", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 5)
        campsiteRepository.add(bobbyCamp)

        val guestUser = User(id = "guest-noah", name = "Noah W", entrant = Entrant("guest-noah"))
        userRepository.add(guestUser)

        val bobbyReservation = Reservation(
            id = "res-bobby",
            entrantId = "guest-noah",
            campsiteId = "camp-bobby",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5)),
            status = ReservationStatus.CONFIRMED
        )
        reservationRepository.add(bobbyReservation)

        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals("res-bobby", viewModel.uiState.value.items[0].reservation.id)

        // Cancel the reservation live
        reservationRepository.cancel("res-bobby")
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

}
