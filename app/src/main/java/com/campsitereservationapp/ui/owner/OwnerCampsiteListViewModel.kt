package com.campsitereservationapp.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.model.Campsite
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * UI state for the owner's campsite list screen.
 *
 * @property campsites list of campsites registered by the current owner
 * @property isLoading true while loading the list
 */
data class OwnerCampsiteListUiState(
    val campsites: List<Campsite> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * ViewModel for the owner's campsite list screen (US 02.03.01).
 *
 * **Purpose:** connects to [CampsiteRepository.observeByOwner] to display
 * all campsites registered by the currently logged-in campsite owner.
 *
 * **Design rationale:** MVVM. Subscribes to live owner campsite data so the list
 * updates automatically whenever campsites are added or edited.
 *
 * **Outstanding issues:** none.
 *
 * @property campsiteRepository the shared repository providing campsite data
 * @property currentUserId ID of the logged-in campsite owner
 */
class OwnerCampsiteListViewModel(
    private val campsiteRepository: CampsiteRepository,
    private val currentUserId: String
) : ViewModel() {

    /**
     * Observable stream emitting campsites registered by [currentUserId].
     */
    val uiState: StateFlow<OwnerCampsiteListUiState> = campsiteRepository
        .observeByOwner(currentUserId)
        .map { campsites -> OwnerCampsiteListUiState(campsites = campsites, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OwnerCampsiteListUiState()
        )

    companion object {
        /**
         * ViewModel factory for [OwnerCampsiteListViewModel] initialized with dependencies from [CampsiteApp.container].
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                OwnerCampsiteListViewModel(
                    app.container.campsiteRepository,
                    app.container.currentUserId
                )
            }
        }
    }
}
