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
 * reservations by owner campsite IDs and selected filter (ACTIVE, PAST, CANCELLED), resolves
 * campsite and entrant names, handles guest name fallbacks, tracks loading state, supports
 * live updates when reservations are cancelled, and switches filters.
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
        // Boundary reservation ending exactly on fixedToday (should stay in ACTIVE)
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
    fun viewModel_pastFilter_showsEndedReservationsExcludesBoundaryAndOtherOwners() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val bobbyCamp = Campsite(id = "camp-bobby", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 5)
        val jillCamp = Campsite(id = "camp-jill", ownerId = "owner-jill", name = "Aspen Hollow", capacity = 5)
        campsiteRepository.add(bobbyCamp)
        campsiteRepository.add(jillCamp)

        // Confirmed past reservation ending before fixedToday (should show in PAST)
        val pastRes = Reservation(
            id = "res-past",
            entrantId = "guest-noah",
            campsiteId = "camp-bobby",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 30)),
            status = ReservationStatus.CONFIRMED
        )
        // Boundary reservation ending on fixedToday (should NOT show in PAST)
        val boundaryRes = Reservation(
            id = "res-boundary",
            entrantId = "guest-noah",
            campsiteId = "camp-bobby",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 10, 25), fixedToday),
            status = ReservationStatus.CONFIRMED
        )
        // Past reservation on Jill's campsite (should NOT show for Bobby)
        val jillPastRes = Reservation(
            id = "res-jill-past",
            entrantId = "guest-noah",
            campsiteId = "camp-jill",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20)),
            status = ReservationStatus.CONFIRMED
        )

        reservationRepository.add(pastRes)
        reservationRepository.add(boundaryRes)
        reservationRepository.add(jillPastRes)

        testScheduler.advanceUntilIdle()

        viewModel.onFilterChange(ReservationFilter.PAST)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ReservationFilter.PAST, state.filter)
        assertEquals(1, state.items.size)
        assertEquals("res-past", state.items[0].reservation.id)
    }

    @Test
    fun viewModel_cancelledFilter_showsCancelledReservationsExcludesOtherOwners() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val bobbyCamp = Campsite(id = "camp-bobby", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 5)
        val jillCamp = Campsite(id = "camp-jill", ownerId = "owner-jill", name = "Aspen Hollow", capacity = 5)
        campsiteRepository.add(bobbyCamp)
        campsiteRepository.add(jillCamp)

        val bobbyCancelledRes = Reservation(
            id = "res-bobby-cancelled",
            entrantId = "guest-noah",
            campsiteId = "camp-bobby",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15)),
            status = ReservationStatus.CANCELLED
        )
        val jillCancelledRes = Reservation(
            id = "res-jill-cancelled",
            entrantId = "guest-noah",
            campsiteId = "camp-jill",
            lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15)),
            status = ReservationStatus.CANCELLED
        )

        reservationRepository.add(bobbyCancelledRes)
        reservationRepository.add(jillCancelledRes)

        testScheduler.advanceUntilIdle()

        viewModel.onFilterChange(ReservationFilter.CANCELLED)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ReservationFilter.CANCELLED, state.filter)
        assertEquals(1, state.items.size)
        assertEquals("res-bobby-cancelled", state.items[0].reservation.id)
    }

    @Test
    fun viewModel_switchingFilters_updatesUiStateAndItems() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val bobbyCamp = Campsite(id = "camp-bobby", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 5)
        campsiteRepository.add(bobbyCamp)

        val activeRes = Reservation(
            id = "res-active",
            entrantId = "guest-noah",
            campsiteId = "camp-bobby",
            dates = DateRange(fixedToday, fixedToday.plusDays(5)),
            status = ReservationStatus.CONFIRMED
        )
        val pastRes = Reservation(
            id = "res-past",
            entrantId = "guest-noah",
            campsiteId = "camp-bobby",
            dates = DateRange(fixedToday.minusDays(10), fixedToday.minusDays(2)),
            status = ReservationStatus.CONFIRMED
        )
        val cancelledRes = Reservation(
            id = "res-cancelled",
            entrantId = "guest-noah",
            campsiteId = "camp-bobby",
            dates = DateRange(fixedToday, fixedToday.plusDays(5)),
            status = ReservationStatus.CANCELLED
        )

        reservationRepository.add(activeRes)
        reservationRepository.add(pastRes)
        reservationRepository.add(cancelledRes)

        testScheduler.advanceUntilIdle()

        // Default: ACTIVE
        assertEquals(ReservationFilter.ACTIVE, viewModel.uiState.value.filter)
        assertEquals(listOf("res-active"), viewModel.uiState.value.items.map { it.reservation.id })

        // Switch to PAST
        viewModel.onFilterChange(ReservationFilter.PAST)
        testScheduler.advanceUntilIdle()
        assertEquals(ReservationFilter.PAST, viewModel.uiState.value.filter)
        assertEquals(listOf("res-past"), viewModel.uiState.value.items.map { it.reservation.id })

        // Switch to CANCELLED
        viewModel.onFilterChange(ReservationFilter.CANCELLED)
        testScheduler.advanceUntilIdle()
        assertEquals(ReservationFilter.CANCELLED, viewModel.uiState.value.filter)
        assertEquals(listOf("res-cancelled"), viewModel.uiState.value.items.map { it.reservation.id })

        // Switch back to ACTIVE
        viewModel.onFilterChange(ReservationFilter.ACTIVE)
        testScheduler.advanceUntilIdle()
        assertEquals(ReservationFilter.ACTIVE, viewModel.uiState.value.filter)
        assertEquals(listOf("res-active"), viewModel.uiState.value.items.map { it.reservation.id })
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
