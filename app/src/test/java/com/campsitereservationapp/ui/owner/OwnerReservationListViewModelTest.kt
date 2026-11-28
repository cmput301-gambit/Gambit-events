package com.campsitereservationapp.ui.owner

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.DataSource
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Entrant
import com.campsitereservationapp.model.Image
import com.campsitereservationapp.model.ImageKind
import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.model.NotificationType
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.model.ReservationStatus
import com.campsitereservationapp.model.User
import com.campsitereservationapp.service.NotificationHandler
import org.junit.Assert.assertNull
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
    private lateinit var imageRepository: ImageRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var viewModel: OwnerReservationListViewModel
    private val fixedToday = LocalDate.of(2026, 11, 1)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepository = CampsiteRepository(MockDataSource())
        reservationRepository = ReservationRepository(MockDataSource())
        userRepository = UserRepository(MockDataSource())
        imageRepository = ImageRepository(MockDataSource())
        notificationRepository = NotificationRepository(MockDataSource())

        viewModel = OwnerReservationListViewModel(
            reservationRepository = reservationRepository,
            campsiteRepository = campsiteRepository,
            userRepository = userRepository,
            imageRepository = imageRepository,
            notificationHandler = NotificationHandler(notificationRepository),
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

    // ---------- Reject a reservation (US 02.08.01) ----------

    private val pineRidge = Campsite(id = "camp-bobby", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 8)
    private val noahStay = Reservation(
        id = "res-noah",
        entrantId = "guest-noah",
        campsiteId = "camp-bobby",
        lotteryId = "lottery-a",
        dates = DateRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12)),
    )

    private suspend fun seedNoahAtPineRidge() {
        campsiteRepository.add(pineRidge)
        userRepository.add(
            User(id = "guest-noah", name = "Noah W", profileImageId = "img-noah", entrant = Entrant("guest-noah"))
        )
        imageRepository.add(
            Image(id = "img-noah", uploaderId = "guest-noah", kind = ImageKind.PROFILE, targetId = "guest-noah", data = "QUJD")
        )
        reservationRepository.add(noahStay)
    }

    @Test
    fun items_includeGuestProfilePictureAndPartySize() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        seedNoahAtPineRidge()
        testScheduler.advanceUntilIdle()

        val item = viewModel.uiState.value.items.single()
        assertEquals("QUJD", item.guestProfileImage)
        assertEquals(8, item.partySize)
    }

    @Test
    fun selectingAReservation_opensItsPopup_andDismissCloses() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        seedNoahAtPineRidge()
        testScheduler.advanceUntilIdle()

        viewModel.onReservationSelected("res-noah")
        testScheduler.advanceUntilIdle()
        assertEquals("Noah W", viewModel.uiState.value.selected?.guestName)

        viewModel.onDismissSelected()
        testScheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.selected)
        assertEquals(ReservationStatus.CONFIRMED, reservationRepository.get("res-noah")?.status)
    }

    @Test
    fun rejectSelected_marksRejected_notifiesEntrant_andMovesToRejectedFilter() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        seedNoahAtPineRidge()
        testScheduler.advanceUntilIdle()

        viewModel.onReservationSelected("res-noah")
        viewModel.rejectSelected("Forest fire nearby")
        testScheduler.advanceUntilIdle()

        assertEquals(ReservationStatus.REJECTED, reservationRepository.get("res-noah")?.status)
        val notice = notificationRepository.getAll().single()
        assertEquals("guest-noah", notice.recipientId)
        assertEquals("owner-bobby", notice.senderId)
        assertEquals(NotificationType.RESERVATION_REJECTED, notice.type)
        assertTrue(notice.message.contains("Pine Ridge"))
        assertTrue(notice.message.endsWith("Reason: Forest fire nearby"))
        assertEquals("Forest fire nearby", reservationRepository.get("res-noah")?.rejectionReason)

        val state = viewModel.uiState.value
        assertNull(state.selected)
        assertEquals("Reservation rejected. The guest has been notified.", state.message)
        assertTrue(state.items.isEmpty())

        viewModel.onFilterChange(ReservationFilter.REJECTED)
        testScheduler.advanceUntilIdle()
        assertEquals("res-noah", viewModel.uiState.value.items.single().reservation.id)
    }

    @Test
    fun rejectSelected_withBlankReason_isRefusedAndKeepsThePopupOpen() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        seedNoahAtPineRidge()
        testScheduler.advanceUntilIdle()

        viewModel.onReservationSelected("res-noah")
        viewModel.rejectSelected("   ")
        testScheduler.advanceUntilIdle()

        assertEquals(ReservationStatus.CONFIRMED, reservationRepository.get("res-noah")?.status)
        assertTrue(notificationRepository.getAll().isEmpty())
        assertEquals("Write a reason for rejecting this reservation.", viewModel.uiState.value.message)
        assertEquals("res-noah", viewModel.uiState.value.selected?.reservation?.id)
    }

    @Test
    fun rejectSelected_alreadyCancelled_isRefusedAndSendsNothing() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        seedNoahAtPineRidge()
        reservationRepository.cancel("res-noah")
        testScheduler.advanceUntilIdle()

        viewModel.onReservationSelected("res-noah")
        viewModel.rejectSelected("Forest fire nearby")
        testScheduler.advanceUntilIdle()

        assertEquals(ReservationStatus.CANCELLED, reservationRepository.get("res-noah")?.status)
        assertTrue(notificationRepository.getAll().isEmpty())
        assertEquals("This reservation was already cancelled or rejected.", viewModel.uiState.value.message)
    }

    @Test
    fun rejectSelected_atAnotherOwnersCampsite_isRefused() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        campsiteRepository.add(Campsite(id = "camp-jill", ownerId = "owner-jill", name = "Aspen Hollow", capacity = 5))
        reservationRepository.add(noahStay.copy(id = "res-jill", campsiteId = "camp-jill"))
        testScheduler.advanceUntilIdle()

        viewModel.onReservationSelected("res-jill")
        viewModel.rejectSelected("Forest fire nearby")
        testScheduler.advanceUntilIdle()

        assertEquals(ReservationStatus.CONFIRMED, reservationRepository.get("res-jill")?.status)
        assertTrue(notificationRepository.getAll().isEmpty())
    }

    @Test
    fun items_onlyAllowRejectingStaysThatHaventEnded() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        seedNoahAtPineRidge()
        reservationRepository.add(
            noahStay.copy(id = "res-old", dates = DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)))
        )
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.items.single().canReject)

        viewModel.onFilterChange(ReservationFilter.PAST)
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.items.single().canReject)
    }

    @Test
    fun rejectSelected_aStayThatAlreadyEnded_isRefusedAndSendsNothing() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        seedNoahAtPineRidge()
        reservationRepository.add(
            noahStay.copy(id = "res-old", dates = DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)))
        )
        testScheduler.advanceUntilIdle()

        viewModel.onReservationSelected("res-old")
        viewModel.rejectSelected("Forest fire nearby")
        testScheduler.advanceUntilIdle()

        assertEquals(ReservationStatus.CONFIRMED, reservationRepository.get("res-old")?.status)
        assertTrue(notificationRepository.getAll().isEmpty())
        assertEquals("This stay has already ended, so it can't be rejected.", viewModel.uiState.value.message)
    }

    @Test
    fun rejectSelected_whenTheNoticeFails_saysItWasRejectedButNotSent() = runTest {
        val failingNotices = object : DataSource<Notification> by MockDataSource() {
            override suspend fun add(item: Notification) = throw IllegalStateException("offline")
        }
        val vm = OwnerReservationListViewModel(
            reservationRepository = reservationRepository,
            campsiteRepository = campsiteRepository,
            userRepository = userRepository,
            imageRepository = imageRepository,
            notificationHandler = NotificationHandler(NotificationRepository(failingNotices)),
            currentUserId = "owner-bobby",
            today = fixedToday,
        )
        backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }
        seedNoahAtPineRidge()
        testScheduler.advanceUntilIdle()

        vm.onReservationSelected("res-noah")
        vm.rejectSelected("Forest fire nearby")
        testScheduler.advanceUntilIdle()

        assertEquals(ReservationStatus.REJECTED, reservationRepository.get("res-noah")?.status)
        assertEquals(
            "Reservation rejected, but the guest couldn't be notified. Message them directly.",
            vm.uiState.value.message,
        )
        assertNull(vm.uiState.value.selected)
    }

    @Test
    fun cancelledFilter_excludesRejectedReservations() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        seedNoahAtPineRidge()
        reservationRepository.reject("res-noah", "Forest fire", today = fixedToday)
        viewModel.onFilterChange(ReservationFilter.CANCELLED)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.items.isEmpty())
    }
}
