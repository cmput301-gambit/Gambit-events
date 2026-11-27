package com.campsitereservationapp.ui.campsite

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteError
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Image
import com.campsitereservationapp.model.ImageKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * UI state for the campsite registration/edit form.
 *
 * @property isLoading true while loading existing campsite data for editing
 * @property loadError true if loading existing campsite data failed (null or exception)
 * @property isSaving true while the campsite is being saved to the repository
 * @property saveError true if saving to the repository failed with an exception
 * @property isSaved true if the campsite was successfully saved
 * @property name display name of the campsite
 * @property description description / amenities of the campsite
 * @property location textual location of the campsite
 * @property capacity maximum occupancy entered as a string
 * @property tags list of tags associated with the campsite
 * @property posterImage the poster currently shown in the form, as Base64 JPEG text
 *   ([Image.data]), or null for no poster
 * @property latitude optional latitude map coordinate
 * @property longitude optional longitude map coordinate
 * @property availability optional reservation availability date range
 * @property errors list of validation errors encountered during save attempts
 */
data class CampsiteFormUiState(
    val isLoading: Boolean = false,
    val loadError: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val isSaved: Boolean = false,
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val capacity: String = "",
    val tags: List<String> = emptyList(),
    val posterImage: String? = null,
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
 * persisting new or updated campsites with stable ID generation for retry safety, preserving
 * unedited fields via copy, handling async load and save errors safely, and promoting users
 * to campsite owners upon registration.
 *
 * The poster is uploaded to [ImageRepository] when the form is saved, and the
 * campsite keeps only its ID, so every device can see it. A replaced or
 * removed poster is deleted after the campsite is saved.
 *
 * **Outstanding issues:** None.
 *
 * @property campsiteRepository the shared repository campsites are added/updated in
 * @property userRepository the shared repository users are managed in
 * @property imageRepository the shared repository campsite posters are uploaded to
 * @property currentUserId ID of the owner registering or updating the campsite
 * @property editingCampsiteId optional ID of an existing campsite being edited, or null for creation
 */
class CampsiteFormViewModel(
    private val campsiteRepository: CampsiteRepository,
    private val userRepository: UserRepository,
    private val imageRepository: ImageRepository,
    private val currentUserId: String,
    private val editingCampsiteId: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampsiteFormUiState(isLoading = editingCampsiteId != null))

    /**
     * Observable stream of the current form UI state.
     */
    val uiState: StateFlow<CampsiteFormUiState> = _uiState.asStateFlow()

    private var originalCampsite: Campsite? = null

    // Stable ID generation across save retries
    private val newCampsiteId by lazy { campsiteRepository.newId() }

    // True once the owner picks or removes a poster, so an unchanged poster isn't re-uploaded.
    private var posterChanged = false

    // ID for the newly picked poster; a new one per pick, kept across save retries.
    private var newPosterId: String? = null

    init {
        if (editingCampsiteId != null) {
            viewModelScope.launch {
                try {
                    val loaded = campsiteRepository.get(editingCampsiteId)
                    originalCampsite = loaded
                    if (loaded != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadError = false,
                                name = loaded.name,
                                description = loaded.description,
                                location = loaded.location,
                                capacity = loaded.capacity.toString(),
                                tags = loaded.tags,
                                posterImage = loadPoster(loaded.posterImageId),
                                latitude = loaded.latitude,
                                longitude = loaded.longitude,
                                availability = loaded.availability
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, loadError = true) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, loadError = true) }
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
     * Shows a newly picked poster in the form. It's uploaded when the form is saved.
     *
     * @param jpegBytes the shrunk JPEG picture, from
     *   [com.campsitereservationapp.ui.components.loadJpegBytes]
     */
    fun onPosterImagePicked(jpegBytes: ByteArray) {
        posterChanged = true
        newPosterId = imageRepository.newId()
        _uiState.update { it.copy(posterImage = Image.encode(jpegBytes), saveError = false) }
    }

    /**
     * Removes the poster from the form. The old poster is deleted when the form is saved.
     */
    fun onPosterImageRemoved() {
        posterChanged = true
        newPosterId = null
        _uiState.update { it.copy(posterImage = null) }
    }

    // Loads a saved poster's picture. Old campsites may hold a phone-only
    // link instead of an image ID; those show no poster.
    private suspend fun loadPoster(imageId: String?): String? = try {
        imageRepository.find(imageId)?.data
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Couldn't load poster $imageId", e)
        null
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
     * to the repository (adding a new one or updating an existing one, preserving unedited fields
     * and maintaining stable IDs for retry safety). Prevents saving if loading is active or
     * encountered a load error. Promotes the user to a campsite owner upon new registration.
     * Sets [CampsiteFormUiState.isSaving] during the operation and [CampsiteFormUiState.isSaved]
     * upon success, or [CampsiteFormUiState.saveError] on exception.
     */
    fun save() {
        val state = _uiState.value
        if (state.isLoading || state.loadError || state.isSaving || state.isSaved) return

        val parsedCapacity = state.capacity.toIntOrNull()
        val baseCampsite = originalCampsite ?: Campsite(
            id = newCampsiteId,
            ownerId = currentUserId
        )

        val oldPosterId = originalCampsite?.posterImageId
        val poster = if (posterChanged && state.posterImage != null) {
            Image(
                id = newPosterId ?: imageRepository.newId().also { newPosterId = it },
                uploaderId = baseCampsite.ownerId,
                kind = ImageKind.CAMPSITE,
                targetId = baseCampsite.id,
                data = state.posterImage,
            )
        } else {
            null
        }
        val posterImageId = if (posterChanged) poster?.id else oldPosterId

        val campsite = baseCampsite.copy(
            name = state.name.trim(),
            description = state.description.trim(),
            location = state.location.trim(),
            capacity = parsedCapacity ?: 0,
            tags = state.tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            posterImageId = posterImageId,
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
        if (poster != null && !poster.isValid()) {
            _uiState.update { it.copy(saveError = true) }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveError = false) }
        viewModelScope.launch {
            try {
                // Upload first so the campsite never points at a missing poster.
                // Checked first so a retry doesn't upload it twice.
                if (poster != null && imageRepository.get(poster.id) == null) {
                    imageRepository.add(poster)
                }
                if (editingCampsiteId != null) {
                    campsiteRepository.update(campsite)
                } else {
                    if (campsiteRepository.get(campsite.id) != null) {
                        campsiteRepository.update(campsite)
                    } else {
                        campsiteRepository.add(campsite)
                    }
                    userRepository.promoteToOwner(currentUserId)
                }
                if (oldPosterId != null && oldPosterId != posterImageId) deleteOldPoster(oldPosterId)
                _uiState.update { it.copy(isSaving = false, isSaved = true, errors = emptyList(), saveError = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = true) }
            }
        }
    }

    // The campsite is already saved, so a failure here only leaves an unused
    // picture behind; it's logged rather than shown as a save error.
    private suspend fun deleteOldPoster(imageId: String) {
        try {
            imageRepository.find(imageId)?.let { imageRepository.delete(it.id) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't delete old poster $imageId", e)
        }
    }

    companion object {
        private const val TAG = "CampsiteFormViewModel"

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
                    imageRepository = app.container.imageRepository,
                    currentUserId = app.container.currentUserId,
                    editingCampsiteId = editingCampsiteId
                )
            }
        }

        /** Default factory for creating new campsites. */
        val Factory = provideFactory(null)
    }
}
