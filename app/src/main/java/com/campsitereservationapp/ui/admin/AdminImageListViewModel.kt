package com.campsitereservationapp.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Image
import com.campsitereservationapp.service.CascadeDeleter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * One uploaded image as the admin sees it: the image plus the uploader's name.
 *
 * @property image the image itself
 * @property uploaderName the uploader's name; null if they haven't set one or their profile is gone
 */
data class AdminImageRow(
    val image: Image,
    val uploaderName: String? = null,
)

/**
 * UI state for the admin image list screen.
 *
 * @property images every uploaded image, grouped by uploader
 * @property isLoading true until the first list of images arrives
 * @property imagePendingRemoval the image the admin tapped Remove on, waiting
 * for them to confirm; null when no popup is showing
 * @property errorMessage shown when a removal failed, for example with no network
 */
data class AdminImageListUiState(
    val images: List<AdminImageRow> = emptyList(),
    val isLoading: Boolean = true,
    val imagePendingRemoval: AdminImageRow? = null,
    val errorMessage: String? = null,
)

/**
 * ViewModel for the admin image list screen (US 03.06.01, 03.03.01).
 *
 * **Purpose:** lets an admin browse every uploaded picture (campsite posters
 * and profile pictures), see who uploaded each one, and remove ones that
 * break the app's rules after confirming in a popup.
 *
 * **Design rationale:** MVVM, following [AdminRatingListViewModel]. Combines
 * the live images and users streams so each image shows its uploader's name
 * and user ID, and the list updates as soon as anything changes. Images are
 * grouped by uploader (named uploaders first) so the admin can spot one
 * person uploading many bad pictures. Removal goes through
 * [CascadeDeleter.deleteImage] so the campsite or profile that showed the
 * picture stops pointing at it.
 *
 * **Outstanding issues:** the admin screens don't yet check that the current
 * user is actually an admin. The uploader isn't told when their picture is removed.
 *
 * @property imageRepository the shared repository providing uploaded images
 * @property userRepository the shared repository providing uploaders' names
 * @property cascadeDeleter removes an image and clears it from its campsite or profile
 */
class AdminImageListViewModel(
    private val imageRepository: ImageRepository,
    private val userRepository: UserRepository,
    private val cascadeDeleter: CascadeDeleter,
) : ViewModel() {

    private val pendingRemoval = MutableStateFlow<AdminImageRow?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    /** Live screen state: every uploaded image with its uploader's name, plus any popup or error. */
    val uiState: StateFlow<AdminImageListUiState> =
        combine(
            imageRepository.observeAll(),
            userRepository.observeAll(),
            pendingRemoval,
            errorMessage,
        ) { images, users, pending, error ->
            val userNames = users.associate { it.id to it.name.ifBlank { null } }
            AdminImageListUiState(
                images = images
                    .map { AdminImageRow(image = it, uploaderName = userNames[it.uploaderId]) }
                    .sortedWith(
                        compareBy(
                            { it.uploaderName == null },
                            { it.uploaderName?.lowercase() },
                            { it.image.uploaderId },
                            { it.image.kind },
                        )
                    ),
                isLoading = false,
                imagePendingRemoval = pending,
                errorMessage = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminImageListUiState(),
        )

    /**
     * Called when the admin taps Remove. Opens the popup instead of deleting straight away.
     *
     * @param row the image the admin wants to remove
     */
    fun requestRemoval(row: AdminImageRow) {
        pendingRemoval.value = row
    }

    /** Called when the admin taps Cancel or closes the popup. Nothing is removed. */
    fun cancelRemoval() {
        pendingRemoval.value = null
    }

    /** Hides the error message. */
    fun dismissError() {
        errorMessage.value = null
    }

    /**
     * Called when the admin confirms. Deletes the image waiting in the popup
     * (clearing it from its campsite or profile) and closes the popup.
     * Does nothing if no removal is pending.
     */
    fun confirmRemoval() {
        val row = pendingRemoval.value ?: return
        pendingRemoval.value = null
        viewModelScope.launch {
            try {
                cascadeDeleter.deleteImage(row.image.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove image ${row.image.id}", e)
                errorMessage.value = "Couldn't remove the photo. Check your connection and try again."
            }
        }
    }

    companion object {
        private const val TAG = "AdminImageList"

        /**
         * Factory providing [AdminImageListViewModel] initialized with [CampsiteApp.container].
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                AdminImageListViewModel(
                    app.container.imageRepository,
                    app.container.userRepository,
                    app.container.cascadeDeleter,
                )
            }
        }
    }
}
