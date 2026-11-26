package com.campsitereservationapp.ui.messages

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.model.ReservationStatus
import com.campsitereservationapp.model.User
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [OwnerGuestsViewModel].
 *
 * **Purpose:** checks that an owner sees only the active, upcoming guests of
 * the campsite they picked, with names, so they can message them (US 02.12.01),
 * and that someone who doesn't own the campsite, or a campsite that no longer
 * exists, gets a blocked message instead of the list.
 *
 * **Design rationale:** repositories backed by [MockDataSource] and a fixed
 * "today", so the tests never touch Firestore and behave the same on any day.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerGuestsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 12, 1)
    private lateinit var reservationRepository: ReservationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var campsiteRepository: CampsiteRepository

    private fun viewModelFor(userId: String = "owner-bobby", day: LocalDate = today) =
        OwnerGuestsViewModel(reservationRepository, userRepository, campsiteRepository, userId, day)

    private fun reservation(id: String, entrantId: String, campsiteId: String, startDay: Int, finishDay: Int) =
        Reservation(
            id = id, entrantId = entrantId, campsiteId = campsiteId, lotteryId = "lottery-a",
            dates = DateRange(LocalDate.of(2026, 12, startDay), LocalDate.of(2026, 12, finishDay)),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        reservationRepository = ReservationRepository(
            MockDataSource(
                listOf(
                    reservation("r-late", "entrant-ali", "campsite-pine-ridge", 20, 22),
                    reservation("r-soon", "entrant-priya", "campsite-pine-ridge", 5, 7),
                    reservation("r-cancelled", "entrant-ben", "campsite-pine-ridge", 10, 12)
                        .copy(status = ReservationStatus.CANCELLED),
                    reservation("r-other", "entrant-sara", "campsite-lakeside", 5, 7),
                )
            )
        )
        userRepository = UserRepository(MockDataSource(listOf(User(id = "entrant-priya", name = "Priya"))))
        campsiteRepository = CampsiteRepository(
            MockDataSource(
                listOf(
                    Campsite(id = "campsite-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge"),
                    Campsite(id = "campsite-empty", ownerId = "owner-bobby", name = "Empty Meadow"),
                )
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsOnlyThisCampsitesActiveGuests_earliestFirst() = runTest {
        val viewModel = viewModelFor()

        viewModel.load("campsite-pine-ridge")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("r-soon", "r-late"), state.guests.map { it.reservationId })
        assertEquals("2026-12-05 to 2026-12-07", state.guests.first().dates)
    }

    @Test
    fun guestsShowTheirName_orGuestIfTheyHaveNone() = runTest {
        val viewModel = viewModelFor()

        viewModel.load("campsite-pine-ridge")
        advanceUntilIdle()

        assertEquals(listOf("Priya", "Guest"), viewModel.uiState.value.guests.map { it.guestName })
    }

    @Test
    fun pastReservations_areLeftOut() = runTest {
        val later = LocalDate.of(2026, 12, 15)
        val viewModel = viewModelFor(day = later)

        viewModel.load("campsite-pine-ridge")
        advanceUntilIdle()

        assertEquals(listOf("r-late"), viewModel.uiState.value.guests.map { it.reservationId })
    }

    @Test
    fun campsiteWithNoReservations_isEmpty() = runTest {
        val viewModel = viewModelFor()

        viewModel.load("campsite-empty")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.guests.isEmpty())
    }

    @Test
    fun nonOwner_isBlocked_andSeesNoGuests() = runTest {
        val viewModel = viewModelFor(userId = "entrant-ali")

        viewModel.load("campsite-pine-ridge")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Only this campsite's owner can message its guests.", state.blockedReason)
        assertFalse(state.isLoading)
        assertTrue(state.guests.isEmpty())
    }

    @Test
    fun missingCampsite_isBlocked_notStuckLoading() = runTest {
        val viewModel = viewModelFor()

        viewModel.load("campsite-gone")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("This campsite no longer exists.", viewModel.uiState.value.blockedReason)
    }
}