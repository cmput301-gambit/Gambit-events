package com.campsitereservationapp.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteFilter
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.model.Campsite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Everything the campsite list screen needs to draw itself.
 *
 * The screen reads only this class. It never talks to a repository directly.
 *
 * @property campsites the campsites to show, already filtered by [searchQuery]
 * @property searchQuery the text currently typed in the search bar
 * @property isLoading true until the first list of campsites arrives
 */
data class CampsiteListUiState(
    val campsites: List<Campsite> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
)

/**
 * Holds the state for the entrant's campsite list screen.
 *
 * **Purpose:** shows campsites to entrants and filters them by search text
 * (US 01.01.01, 01.01.03). Reference example of the team's ViewModel pattern.
 *
 * **Design rationale:** MVVM. Receives its repository from the
 * [com.campsitereservationapp.data.AppContainer] through [Factory], and
 * subscribes to the repository's live stream, so the list updates on its own
 * whenever campsite data changes.
 *
 * **Outstanding issues:** filters by location, date and guests
 * (US 01.01.02) not yet added.
 *
 * @property campsiteRepository the shared repository campsites come from
 */
class CampsiteListViewModel(
    private val campsiteRepository: CampsiteRepository,
) : ViewModel() {

    // State that belongs to this screen only: what the user typed.
    // Private and mutable, so only this ViewModel can change it.
    private val searchQuery = MutableStateFlow("")
    private val campsiteFilter : CampsiteFilter = CampsiteFilter()
    /**
     * The current screen state. The screen collects this and redraws
     * every time it changes.
     */
    val uiState: StateFlow<CampsiteListUiState> =
    // combine() listens to two streams at once. Whenever EITHER one
    // changes (new campsite data, or new search text), the block below
        // runs again with the latest value from each.
        combine(
            campsiteRepository.observeAll(), // live campsite list from the repository
            searchQuery,                     // live search text from the user
        ) { campsites, query ->
            // Turn raw data into exactly what the screen needs to draw.
            CampsiteListUiState(
                campsites = campsites.filter { it.name.contains(query, ignoreCase = true) },
                searchQuery = query,
                isLoading = false, // data has arrived, so we're done loading
            )
        }
            // stateIn() turns the stream into a StateFlow the screen can read.
            .stateIn(
                // Tie the subscription to this ViewModel's lifetime.
                // It's cancelled automatically when the ViewModel is cleared.
                scope = viewModelScope,
                // Stop the listener once the screen is gone for 5 seconds
                started = SharingStarted.WhileSubscribed(5_000),
                // What the screen shows before any data arrives.
                // Its isLoading = true lets the screen show a spinner.
                initialValue = CampsiteListUiState(),
            )

    /**
     * Called by the screen whenever the search text changes.
     *
     * Updating [searchQuery] makes combine() recalculate the state,
     * so the filtered list updates automatically.
     *
     * @param query the new search text
     */
    fun onSearch(query: String) {
        searchQuery.value = query
    }

    companion object {
        /**
         * Builds this ViewModel with the shared repository from the AppContainer.
         *
         * Android hands every factory the Application object under
         * APPLICATION_KEY. Because the manifest registers CampsiteApp, we can
         * cast it and reach the container. The ViewModel itself never sees
         * the container; it only receives the repository it needs.
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                CampsiteListViewModel(app.container.campsiteRepository)
            }
        }
    }
}