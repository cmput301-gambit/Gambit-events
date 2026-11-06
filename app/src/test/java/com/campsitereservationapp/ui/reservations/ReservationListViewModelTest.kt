package com.campsitereservationapp.ui.campsite

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.SampleData
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.model.ReservationStatus
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [ReservationListViewModel].
 *
 * **Purpose:** checks that entrants see only their own active, upcoming
 * reservations with campsite names (US 01.06.01), and that cancelling
 * removes one from the list (US 01.07.01).
 *
 * **Design rationale:** repositories backed by [MockDataSource], and a fixed
 * "today", so the tests never touch Firestore and behave the same on any day.
 *
 * **Outstanding issues:** None.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReservationListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 12, 1)
    private lateinit var reservationRepository: ReservationRepository
    private lateinit var campsiteRepository: CampsiteRepository

    private fun reservation(id: String, entrantId: String, start: LocalDate, finish: LocalDate) =
        Reservation(
            id = id, entrantId = entrantId, campsiteId = "campsite-pine-ridge",
            lotteryId = "lottery-a", dates = DateRange(start, finish),
        )

    private val priyasUpcoming =
        reservation("r1", "entrant-priya", LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12))
    private val priyasCancelled =
        reservation("r2", "entrant-priya", LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 21))
            .copy(status = ReservationStatus.CANCELLED)
    private val priyasPast =
        reservation("r3", "entrant-priya", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3))
    private val alisUpcoming =
        reservation("r4", "entrant-ali", LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 16))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        reservationRepository = ReservationRepository(
            MockDataSource(listOf(priyasUpcoming, priyasCancelled, priyasPast, alisUpcoming)))
        campsiteRepository = CampsiteRepository(MockDataSource(SampleData.campsites))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.startCollecting(viewModel: ReservationListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    private fun priyasViewModel() =
        ReservationListViewModel(reservationRepository, campsiteRepository, "entrant-priya", today)

    @Test
    fun showsOnlyMyActiveUpcomingReservations_withCampsiteNames() = runTest {
        val viewModel = priyasViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        val shown = viewModel.uiState.value.reservations
        assertEquals(listOf("r1"), shown.map { it.reservation.id })
        assertEquals("Pine Ridge", shown.single().campsiteName)
    }

    @Test
    fun cancel_removesTheReservationFromTheList() = runTest {
        val viewModel = priyasViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        viewModel.cancel("r1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.reservations.isEmpty())
        assertEquals(ReservationStatus.CANCELLED, reservationRepository.get("r1")?.status)
    }

    @Test
    fun reservationAtDeletedCampsite_stillShows() = runTest {
        campsiteRepository.delete("campsite-pine-ridge")
        val viewModel = priyasViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals("Removed campsite", viewModel.uiState.value.reservations.single().campsiteName)
    }
}