package com.campsitereservationapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.model.Campsite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the admin campsite list screen.
 *
 * @property campsites list of all campsites in the system
 * @property isLoading true while data is loading
 * @property campsitePendingRemoval the campsite the admin tapped Remove on, waiting
 * for them to confirm; null when no confirmation popup is showing
 */
data class AdminCampsiteListUiState(
    val campsites: List<Campsite> = emptyList(),
    val isLoading: Boolean = true,
    val campsitePendingRemoval: Campsite? = null,
)

/**
 * ViewModel for the admin campsite list screen (US 03.04.01, 03.01.01).
 *
 * **Purpose:** allows administrators to observe all campsites in the repository
 * and delete rule-breaking campsites along with their associated lotteries,
 * after confirming in a popup.
 *
 * **Design rationale:** MVVM. Connects directly to [CampsiteRepository.observeAll]
 * so the list updates live whenever a campsite is added, updated, or removed.
 * The campsite waiting for confirmation is kept here rather than in the screen,
 * so the confirm and cancel steps can be unit tested.
 *
 * **Outstanding issues:** the admin screens don't yet check that the current
 * user is actually an admin.
 *
 * @property campsiteRepository the shared repository providing campsite data
 * @property lotteryRepository the shared repository providing lottery data
 */
class AdminCampsiteListViewModel(
    private val campsiteRepository: CampsiteRepository,
    private val lotteryRepository: LotteryRepository
) : ViewModel() {

    // The campsite the admin tapped Remove on, until they confirm or cancel.
    private val pendingRemoval = MutableStateFlow<Campsite?>(null)

    /**
     * Live state stream emitting all campsites in the system and the
     * campsite waiting for removal confirmation, if any.
     */
    val uiState: StateFlow<AdminCampsiteListUiState> =
        combine(campsiteRepository.observeAll(), pendingRemoval) { campsites, pending ->
            AdminCampsiteListUiState(
                campsites = campsites,
                isLoading = false,
                campsitePendingRemoval = pending,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminCampsiteListUiState()
        )

    /**
     * Called when the admin taps Remove. Opens the confirmation popup
     * instead of deleting straight away.
     *
     * @param campsite the campsite the admin wants to remove
     */
    fun requestRemoval(campsite: Campsite) {
        pendingRemoval.value = campsite
    }

    /**
     * Called when the admin taps Cancel or closes the popup. Nothing is deleted.
     */
    fun cancelRemoval() {
        pendingRemoval.value = null
    }

    /**
     * Called when the admin confirms. Deletes the campsite waiting for
     * confirmation and closes the popup. Does nothing if no removal is pending.
     */
    fun confirmRemoval() {
        val campsite = pendingRemoval.value ?: return
        pendingRemoval.value = null
        removeCampsite(campsite.id)
    }

    /**
     * Removes a campsite and its associated lotteries from the repository by its ID.
     *
     * @param campsiteId ID of the campsite to delete
     */
    fun removeCampsite(campsiteId: String) {
        viewModelScope.launch {
            campsiteRepository.delete(campsiteId)
            lotteryRepository.deleteByCampsite(campsiteId)
        }
    }

    companion object {
        /**
         * Factory providing [AdminCampsiteListViewModel] initialized with [CampsiteApp.container].
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                AdminCampsiteListViewModel(
                    app.container.campsiteRepository,
                    app.container.lotteryRepository
                )
            }
        }
    }
}