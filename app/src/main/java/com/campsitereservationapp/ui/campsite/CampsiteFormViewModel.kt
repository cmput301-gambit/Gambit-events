package com.campsitereservationapp.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.DateRange
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * UI state for the campsite registration/edit form.
 *
 * @property isSaving true while the campsite is being saved to the repository
 * @property isSaved true if the campsite was successfully saved
 * @property name display name of the campsite
 * @property description description / amenities of the campsite
 * @property location textual location of the campsite
 * @property capacity maximum occupancy entered as a string
 * @property tags list of tags associated with the campsite
 * @property latitude optional latitude map coordinate
 * @property longitude optional longitude map coordinate
 * @property availability optional reservation availability date range
 * @property errors list of validation errors encountered during save attempts
 */
data class CampsiteFormUiState(
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val capacity: String = "",
    val tags: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val availability: DateRange? = null,
    val errors: List<CampsiteError> = emptyList()
)

/**
 * Holds the state for the campsite registration and editing form.
 *
 * **Purpose:** allows campsite owners to register a new campsite (US 02.01.01, US 02.01.02)
 * or update details about an existing campsite (US 02.01.03 / #20).
 *
 * **Design rationale:** MVVM. Receives its repositories and current user ID from the
 * [com.campsitereservationapp.data.AppContainer] through [Factory], handling validation,
 * persisting new or updated campsites, and promoting users to campsite owners upon registration.
 *
 * **Outstanding issues:** image upload and map picking not yet integrated.
 *
 * @property campsiteRepository the shared repository campsites are added/updated in
 * @property userRepository the shared repository users are managed in
 * @property currentUserId ID of the owner registering or updating the campsite
 * @property editingCampsiteId optional ID of an existing campsite being edited, or null for creation
 */
class CampsiteFormViewModel(
    private val campsiteRepository: CampsiteRepository,
    private val userRepository: UserRepository,
    private val currentUserId: String,
    private val editingCampsiteId: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampsiteFormUiState())

    /**
     * Observable stream of the current form UI state.
     */
    val uiState: StateFlow<CampsiteFormUiState> = _uiState.asStateFlow()

    init {
        if (editingCampsiteId != null) {
            viewModelScope.launch {
                campsiteRepository.get(editingCampsiteId)?.let { campsite ->
                    _uiState.update {
                        it.copy(
                            name = campsite.name,
                            description = campsite.description,
                            location = campsite.location,
                            capacity = campsite.capacity.toString(),
                            tags = campsite.tags,
                            latitude = campsite.latitude,
                            longitude = campsite.longitude,
                            availability = campsite.availability
                        )
                    }
                }
            }
        }
    }

    /**
     * Updates the campsite name input in the form state.
     *
     * @param name new campsite name
     */
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    /**
     * Updates the campsite description/amenities input in the form state.
     *
     * @param description new campsite description
     */
    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    /**
     * Updates the campsite location input in the form state.
     *
     * @param location new campsite location
     */
    fun onLocationChange(location: String) {
        _uiState.update { it.copy(location = location) }
    }

    /**
     * Updates the campsite capacity input in the form state.
     *
     * @param capacity new capacity string
     */
    fun onCapacityChange(capacity: String) {
        _uiState.update { it.copy(capacity = capacity) }
    }

    /**
     * Updates the list of tags associated with the campsite.
     *
     * @param tags new list of tags
     */
    fun onTagsChange(tags: List<String>) {
        _uiState.update { it.copy(tags = tags) }
    }

    /**
     * Updates the map location (latitude and longitude) of the campsite.
     *
     * @param lat latitude coordinate, or null
     * @param lng longitude coordinate, or null
     */
    fun onMapLocationChange(lat: Double?, lng: Double?) {
        _uiState.update { it.copy(latitude = lat, longitude = lng) }
    }

    /**
     * Updates the availability date range of the campsite.
     *
     * @param range new date range, or null
     */
    fun onAvailabilityChange(range: DateRange?) {
        _uiState.update { it.copy(availability = range) }
    }

    /**
     * Validates form inputs, trims text values, parses capacity, and saves the campsite
     * to the repository (adding a new one or updating an existing one). Promotes the user
     * to a campsite owner upon new registration. Sets [CampsiteFormUiState.isSaving] during
     * the operation and [CampsiteFormUiState.isSaved] upon success. Populates validation errors otherwise.
     */
    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isSaved) return

        val parsedCapacity = state.capacity.toIntOrNull()
        val campsite = Campsite(
            id = editingCampsiteId ?: campsiteRepository.newId(),
            ownerId = currentUserId,
            name = state.name.trim(),
            description = state.description.trim(),
            location = state.location.trim(),
            capacity = parsedCapacity ?: 0,
            tags = state.tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            latitude = state.latitude,
            longitude = state.longitude,
            availability = state.availability,
        )

        val errors = campsite.validate().toMutableList()
        if (parsedCapacity == null) {
            errors.remove(CampsiteError.NON_POSITIVE_CAPACITY)
            errors.add(CampsiteError.CAPACITY_NOT_A_NUMBER)
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors.distinct()) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                if (editingCampsiteId != null) {
                    campsiteRepository.update(campsite)
                } else {
                    campsiteRepository.add(campsite)
                    userRepository.promoteToOwner(currentUserId)
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true, errors = emptyList()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                // TODO: expose a save-failed error to the UI
            }
        }
    }

    companion object {
        /**
         * Provides a ViewModel factory for [CampsiteFormViewModel], supporting both
         * registration mode and editing mode for an existing campsite.
         */
        fun provideFactory(editingCampsiteId: String? = null) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                CampsiteFormViewModel(
                    campsiteRepository = app.container.campsiteRepository,
                    userRepository = app.container.userRepository,
                    currentUserId = app.container.currentUserId,
                    editingCampsiteId = editingCampsiteId
                )
            }
        }

        /** Default factory for creating new campsites. */
        val Factory = provideFactory(null)
    }
}
