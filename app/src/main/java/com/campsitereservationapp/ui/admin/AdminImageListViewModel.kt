package com.campsitereservationapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Image
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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
 */
data class AdminImageListUiState(
    val images: List<AdminImageRow> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * ViewModel for the admin image list screen (US 03.06.01).
 *
 * **Purpose:** lets an admin browse every uploaded picture (campsite posters
 * and profile pictures) and see who uploaded each one.
 *
 * **Design rationale:** MVVM, following [AdminRatingListViewModel]. Combines
 * the live images and users streams so each image shows its uploader's name
 * and user ID, and the list updates as soon as anything changes. Images are
 * grouped by uploader (named uploaders first) so the admin can spot one
 * person uploading many bad pictures.
 *
 * **Outstanding issues:** the admin screens don't yet check that the current
 * user is actually an admin. Removing images is US 03.03.01 (Avnil), which can
 * call [com.campsitereservationapp.service.CascadeDeleter.deleteImage].
 *
 * @property imageRepository the shared repository providing uploaded images
 * @property userRepository the shared repository providing uploaders' names
 */
class AdminImageListViewModel(
    private val imageRepository: ImageRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    /** Live screen state: every uploaded image with its uploader's name. */
    val uiState: StateFlow<AdminImageListUiState> =
        combine(imageRepository.observeAll(), userRepository.observeAll()) { images, users ->
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
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminImageListUiState(),
        )

    companion object {
        /**
         * Factory providing [AdminImageListViewModel] initialized with [CampsiteApp.container].
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                AdminImageListViewModel(
                    app.container.imageRepository,
                    app.container.userRepository,
                )
            }
        }
    }
}
