package com.campsitereservationapp.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteFilter
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything the entrant home screen needs to properly address the current user.
 *
 * The screen reads only this class. It never talks to a repository directly.
 *
 * @property entrantName determines the text displayed in the "Welcome back, " textbox.
 */
data class EntrantSettingsState(
    val currentUser: User = User(),
    val isUpdateUserOpen: Boolean = false
)

/**
 * Holds the state for the entrant home screen.
 *
 * **Purpose:** provides an entry point for many subsequent screens covering most of the
 * entrant User Stories.
 *
 * **Design rationale:** MVVM. Receives its repository and user ID from the
 * [com.campsitereservationapp.data.AppContainer] through [Factory], and
 * subscribes to the repository's live stream, so the screen adapts correctly to the current user.
 *
 * **Outstanding issues:** Currently the default screen, when it should be determined by the
 * current users roles.
 *
 * @property userRepository The shared repository users come from
 * @property currentUserId The id of the current user as a string
 */
class EntrantSettingsViewModel(
    private val userRepository: UserRepository,
    private val currentUserId: String,
) : ViewModel() {

    //Name to be displayed on screen
    private val currentUser : MutableStateFlow<User> = MutableStateFlow(
        User()
    )

    private val isUpdateUserOpen : MutableStateFlow<Boolean> = MutableStateFlow(
        false
    )
    //Updating the user based on values from the userRepository. When the first time launch screen
    //is done, this should always return a real name instead of a blank or the device ID.
    init {
        viewModelScope.launch {
            val user: User = userRepository.getOrCreate(currentUserId)
            currentUser.update { user }
        }
    }

    /**
     * The current screen state. The screen collects this and redraws
     * every time it changes.
     */
    val uiState: StateFlow<EntrantSettingsState> =

        combine(
            userRepository.observe(currentUserId),
            isUpdateUserOpen,
        ) { newUser, updateOpen ->
            EntrantSettingsState(
                currentUser = newUser ?: User(),
                isUpdateUserOpen = updateOpen,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = EntrantSettingsState(),
            )

    fun updateUserDetails(
            newName: String? = null,
            newPhone: String? = null,
            newEmail: String? = null,
            newProfileImageId: String? = null,
            newNotificationsEnabled: Boolean? = null,
            ){
        viewModelScope.launch {
                val current = userRepository.getOrCreate(currentUserId)
                //TODO: Currently this line updates only locally, should update the user in the db instead
                userRepository.update(
                    current.copy(
                        name = newName ?: current.name,
                        phone = newPhone ?: current.phone,
                        email = newEmail ?: current.email,
                        profileImageId = newProfileImageId ?: current.profileImageId,
                        notificationsEnabled = newNotificationsEnabled ?: current.notificationsEnabled
                    )
                )
            }
        }

    fun toggleReceiveNotifications(){
        viewModelScope.launch {
            updateUserDetails(
                newNotificationsEnabled = !userRepository.getOrCreate(currentUserId).notificationsEnabled
            )
        }
    }

    fun toggleUpdateUserOpen(){
        isUpdateUserOpen.update { !it }
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
                EntrantSettingsViewModel(
                    app.container.userRepository,
                    app.container.currentUserId
                )
            }
        }
    }
}