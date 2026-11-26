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
import kotlinx.coroutines.flow.collect
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
    private val priyasUpcomingCancellation =
        reservation("r5", "entrant-priya", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 12))
    private val priyasUpcomingCancellationNoCSV =
        reservation("r6", "entrant-priya", LocalDate.of(2027, 1, 13), LocalDate.of(2027, 1, 15))
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

    @Test
    fun successfulCancel_withSendCsvOn_setsCsvRequestedTrue() = runTest {
        val viewModel = priyasViewModel()

        startCollecting(viewModel)

        reservationRepository.reserve(priyasUpcomingCancellation)

        advanceUntilIdle()

        viewModel.onToggleReceiveCSV(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.sendCSV)
        viewModel.cancel(priyasUpcomingCancellation.id)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.csvRequested)

        viewModel.onCsvLaunched()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.csvRequested)
    }

    @Test
    fun successfulReserve_withSendCsvOff_setsCsvRequestedFalse() = runTest {
        val viewModel = priyasViewModel()

        startCollecting(viewModel)

        reservationRepository.reserve(priyasUpcomingCancellationNoCSV)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.sendCSV)
        viewModel.cancel(priyasUpcomingCancellationNoCSV.id)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.csvRequested)

        viewModel.onCsvLaunched()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.csvRequested)
    }

    @Test
    fun pastFilter_showsEndedReservationsNewestFirst() = runTest {
        // Ended Nov 22, newer than r3 (ended Nov 3)
        reservationRepository.add(
            reservation("r7", "entrant-priya", LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22))
        )
        val viewModel = priyasViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        viewModel.onShowPastChange(true)
        advanceUntilIdle()

        assertEquals(listOf("r7", "r3"), viewModel.uiState.value.reservations.map { it.reservation.id })
    }

    @Test
    fun pastFilter_excludesReservationEndingToday() = runTest {
        // Ends exactly on `today`, so it is still active, not past
        reservationRepository.add(
            reservation("r8", "entrant-priya", LocalDate.of(2026, 11, 28), today)
        )
        val viewModel = priyasViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        viewModel.onShowPastChange(true)
        advanceUntilIdle()

        val ids = viewModel.uiState.value.reservations.map { it.reservation.id }
        assertFalse("r8" in ids)
        assertEquals(listOf("r3"), ids)
    }

    @Test
    fun activeFilter_keepsPastReservationsHidden() = runTest {
        val viewModel = priyasViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showPast)
        assertEquals(listOf("r1"), state.reservations.map { it.reservation.id })
    }

    @Test
    fun switchingBetweenPastAndActive_updatesShowPastAndReservations() = runTest {
        val viewModel = priyasViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        viewModel.onShowPastChange(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showPast)
        assertEquals(listOf("r3"), viewModel.uiState.value.reservations.map { it.reservation.id })

        viewModel.onShowPastChange(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showPast)
        assertEquals(listOf("r1"), viewModel.uiState.value.reservations.map { it.reservation.id })
    }
}