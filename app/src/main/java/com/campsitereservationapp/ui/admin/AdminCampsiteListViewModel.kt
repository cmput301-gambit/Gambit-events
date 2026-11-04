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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the admin campsite list screen.
 *
 * @property campsites list of all campsites in the system
 * @property isLoading true while data is loading
 */
data class AdminCampsiteListUiState(
    val campsites: List<Campsite> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * ViewModel for the admin campsite list screen (US 03.04.01).
 *
 * **Purpose:** allows administrators to observe all campsites in the repository
 * and delete rule-breaking campsites along with their associated lotteries.
 *
 * **Design rationale:** MVVM. Connects directly to [CampsiteRepository.observeAll]
 * so the list updates live whenever a campsite is added, updated, or removed.
 *
 * **Outstanding issues:** none.
 *
 * @property campsiteRepository the shared repository providing campsite data
 * @property lotteryRepository the shared repository providing lottery data
 */
class AdminCampsiteListViewModel(
    private val campsiteRepository: CampsiteRepository,
    private val lotteryRepository: LotteryRepository
) : ViewModel() {

    /**
     * Live state stream emitting all campsites in the system.
     */
    val uiState: StateFlow<AdminCampsiteListUiState> = campsiteRepository.observeAll()
        .map { campsites -> AdminCampsiteListUiState(campsites = campsites, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminCampsiteListUiState()
        )

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
