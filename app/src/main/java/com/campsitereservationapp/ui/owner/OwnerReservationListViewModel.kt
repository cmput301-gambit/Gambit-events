package com.campsitereservationapp.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.model.ReservationStatus
import com.campsitereservationapp.service.NotificationHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Filter options for the owner reservation list screen.
 *
 * @property label text shown in dropdown
 */
enum class ReservationFilter(val label: String) {
    ACTIVE("Active"),
    PAST("Past"),
    CANCELLED("Cancelled"),
    REJECTED("Rejected"),
}

/**
 * Owner reservation list item representation.
 *
 * @property reservation the reservation entity
 * @property campsiteName resolved display name of the campsite
 * @property guestName resolved display name of the entrant/guest
 * @property guestProfileImage the guest's profile picture as Base64 JPEG text, or null if none
 * @property partySize how many people the campsite holds, shown as the party size
 * @property canReject true if the owner can still reject it: active and not yet ended
 */
data class OwnerReservationItem(
    val reservation: Reservation,
    val campsiteName: String,
    val guestName: String,
    val guestProfileImage: String? = null,
    val partySize: Int? = null,
    val canReject: Boolean = false,
)

/**
 * UI state for owner reservation list screen.
 *
 * @property items filtered list of owner reservation items
 * @property filter current selected filter
 * @property isLoading true while loading data
 * @property selected the reservation whose popup is open, or null if none
 * @property isRejecting true while a rejection is being saved
 * @property message feedback about the last rejection, or null
 */
data class OwnerReservationListUiState(
    val items: List<OwnerReservationItem> = emptyList(),
    val filter: ReservationFilter = ReservationFilter.ACTIVE,
    val isLoading: Boolean = true,
    val selected: OwnerReservationItem? = null,
    val isRejecting: Boolean = false,
    val message: String? = null,
)

/**
 * ViewModel for the campsite owner reservation list (US 02.07.01, 02.08.01).
 *
 * **Purpose:** displays active, past, cancelled or rejected reservations for campsites owned
 * by the logged-in owner, with each guest's name and profile picture. Tapping a reservation
 * opens a popup (mockup M34) where the owner can reject it with a written reason, for example
 * an emergency like a forest fire, and the entrant gets a notice with that reason in their
 * inbox (US 02.08.01).
 *
 * **Design rationale:** MVVM. Combines live streams from [ReservationRepository],
 * [CampsiteRepository], [UserRepository] and [ImageRepository] to filter reservations by owner
 * campsite IDs and the selected filter. Rejecting calls the named repository action
 * [ReservationRepository.reject] (a transaction), and only after it succeeds sends the notice
 * through [NotificationHandler], so a transaction retry never sends a duplicate.
 *
 * **Outstanding issues:** party size is the campsite's capacity, since reservations don't
 * record a guest count yet. [ImageRepository.observeAll] downloads every picture, which is
 * fine at course scale.
 *
 * @property reservationRepository shared repository providing reservation records
 * @property campsiteRepository shared repository providing campsite records
 * @property userRepository shared repository providing user profile records
 * @property imageRepository shared repository providing profile pictures
 * @property notificationHandler sends the rejection notice to the entrant
 * @property currentUserId ID of the currently logged-in campsite owner
 * @property today reference date for filtering active upcoming reservations
 */
class OwnerReservationListViewModel(
    private val reservationRepository: ReservationRepository,
    private val campsiteRepository: CampsiteRepository,
    private val userRepository: UserRepository,
    private val imageRepository: ImageRepository,
    private val notificationHandler: NotificationHandler,
    private val currentUserId: String,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val filter = MutableStateFlow(ReservationFilter.ACTIVE)
    private val selectedId = MutableStateFlow<String?>(null)
    private val isRejecting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val listItems = combine(
        reservationRepository.observeAll(),
        campsiteRepository.observeAll(),
        userRepository.observeAll(),
        imageRepository.observeAll(),
    ) { reservations, campsites, users, images ->
        val campsitesById = campsites.associateBy { it.id }
        val usersById = users.associateBy { it.id }
        val imageData = images.associate { it.id to it.data }
        reservations
            .filter { campsitesById[it.campsiteId]?.ownerId == currentUserId }
            .map { res ->
                val campsite = campsitesById[res.campsiteId]
                val guest = usersById[res.entrantId]
                OwnerReservationItem(
                    reservation = res,
                    campsiteName = campsite?.name?.ifBlank { null } ?: "Campsite",
                    guestName = guest?.name?.ifBlank { null } ?: "Guest",
                    guestProfileImage = guest?.profileImageId?.let { imageData[it] },
                    partySize = campsite?.capacity?.takeIf { it > 0 },
                    canReject = res.canBeRejected(today),
                )
            }
    }

    /**
     * Observable state stream emitting owner reservations matching current owner and filter.
     */
    val uiState: StateFlow<OwnerReservationListUiState> = combine(
        listItems,
        filter,
        selectedId,
        isRejecting,
        message,
    ) { all, f, selected, rejecting, msg ->
        val filteredItems = all
            .filter { item ->
                val res = item.reservation
                when (f) {
                    ReservationFilter.ACTIVE -> res.isActive && !res.dates.finish.isBefore(today)
                    ReservationFilter.PAST -> res.isActive && res.dates.finish.isBefore(today)
                    ReservationFilter.CANCELLED -> res.status == ReservationStatus.CANCELLED
                    ReservationFilter.REJECTED -> res.status == ReservationStatus.REJECTED
                }
            }
            .sortedBy { it.reservation.dates.start }

        OwnerReservationListUiState(
            items = filteredItems,
            filter = f,
            isLoading = false,
            selected = all.firstOrNull { it.reservation.id == selected },
            isRejecting = rejecting,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OwnerReservationListUiState(),
    )

    /**
     * Switches between the different filters (Active, Past, Cancelled, Rejected).
     *
     * @param f filter owner picks
     */
    fun onFilterChange(f: ReservationFilter) {
        filter.value = f
    }

    /**
     * Opens the popup for a reservation the owner tapped (US 02.08.01).
     *
     * @param reservationId the tapped reservation
     */
    fun onReservationSelected(reservationId: String) {
        selectedId.value = reservationId
        message.value = null
    }

    /** Closes the reservation popup without changing anything. */
    fun onDismissSelected() {
        selectedId.value = null
    }

    /** Clears the feedback message once the screen has shown it. */
    fun onMessageShown() {
        message.value = null
    }

    /**
     * Rejects the reservation shown in the popup and notifies the entrant, with the owner's
     * reason (US 02.08.01). Refuses a blank or too-long reason, and reservations that aren't
     * active or aren't at one of this owner's campsites.
     *
     * @param reason why the owner is rejecting it, such as a forest fire
     */
    fun rejectSelected(reason: String) {
        val id = selectedId.value ?: return
        if (isRejecting.value) return
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) {
            message.value = "Write a reason for rejecting this reservation."
            return
        }
        if (trimmed.length > Reservation.MAX_REASON_LENGTH) {
            message.value = "Keep the reason under ${Reservation.MAX_REASON_LENGTH} characters."
            return
        }
        viewModelScope.launch {
            isRejecting.value = true
            try {
                message.value = rejectNow(id, trimmed)
                selectedId.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message.value = "Couldn't reject the reservation. Try again."
            } finally {
                isRejecting.value = false
            }
        }
    }

    // Returns the message to show the owner.
    private suspend fun rejectNow(reservationId: String, reason: String): String {
        val reservation = reservationRepository.get(reservationId)
            ?: return "This reservation no longer exists."
        val campsite = campsiteRepository.get(reservation.campsiteId)
        if (campsite == null || campsite.ownerId != currentUserId) {
            return "You can only reject reservations at your own campsites."
        }
        if (!reservation.isActive) return "This reservation was already cancelled or rejected."
        if (!reservation.canBeRejected(today)) return "This stay has already ended, so it can't be rejected."
        val rejected = reservationRepository.reject(reservationId, reason, today)
            ?: return "This reservation was already cancelled or rejected."
        // The rejection is saved by now, so a failed notice mustn't read as a failed rejection.
        return try {
            notificationHandler.sendReservationRejected(
                rejected, campsite.name.ifBlank { "your campsite" }, currentUserId, today
            )
            "Reservation rejected. The guest has been notified."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "Reservation rejected, but the guest couldn't be notified. Message them directly."
        }
    }

    companion object {
        /**
         * Factory building [OwnerReservationListViewModel] with dependencies from [CampsiteApp.container].
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                OwnerReservationListViewModel(
                    app.container.reservationRepository,
                    app.container.campsiteRepository,
                    app.container.userRepository,
                    app.container.imageRepository,
                    app.container.notificationHandler,
                    app.container.currentUserId,
                )
            }
        }
    }
}
