package com.campsitereservationapp.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.R
import com.campsitereservationapp.model.User
import com.campsitereservationapp.ui.components.loadJpegBytes
import com.campsitereservationapp.ui.components.rememberImageBitmap
import com.campsitereservationapp.ui.theme.CampsiteReservationAppTheme
import kotlinx.coroutines.launch

/**
 * Screen coinciding with M11
 *
 */
@Composable
fun EntrantSettingsScreen(
    navController: NavController,
    viewModel: EntrantSettingsViewModel = viewModel(factory = EntrantSettingsViewModel.Companion.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    val imageBitmap = rememberImageBitmap(state.currentProfileImage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The photo is shrunk and read right away, so no long-lived URI permission is needed.
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                loadJpegBytes(context, uri)?.let { viewModel.onProfilePicturePicked(it) }
            }
        }
    }

    EntrantSettingsContent(
        state = state,
        onBack = { navController.popBackStack() },
        onToggleUpdateUser = viewModel::toggleUpdateUserOpen,
        onSaveProfile = viewModel::updateUserDetails,
//        onSaveProfile = { name?, phone?, email? ->
//            viewModel.updateUserDetails(
//                newName = name,
//                newPhone = phone,
//                newEmail = email,
//            )
//            viewModel.toggleUpdateUserOpen()
//        },
        onToggleNotifications = viewModel::toggleReceiveNotifications,
        onPolicyClick = { navController.navigate("PolicyAndGuidelines") },
        onDeleteAccount = viewModel::requestDeleteAccount,
        profileImageBitmap = imageBitmap,
        photoPickerLauncher = photoPickerLauncher,
        onProfilePictureRemoved = viewModel::onProfilePictureRemoved
    )

    // "Are you sure?" popup for deleting the account (US 01.08.01).
    if (state.isDeleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteAccount,
            title = { Text("Delete your account?") },
            text = {
                Text(
                    "This will permanently delete your profile and take you off every " +
                        "waiting list. Your reservations, messages, notifications and ratings " +
                        "will be deleted too. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteAccount) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteAccount) { Text("Cancel") }
            },
        )
    }

    state.deleteError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteError,
            title = { Text("Something went wrong") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDeleteError) { Text("OK") }
            },
        )
    }

    // Once the profile is gone, leave settings and clear the back stack so
    // Back can't return to a deleted profile.
    LaunchedEffect(state.accountDeleted) {
        if (state.accountDeleted) {
            navController.navigate("EntrantHome") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }
}

@Composable
fun EntrantSettingsContent(
    state: EntrantSettingsState,
    profileImageBitmap: ImageBitmap?,
    onBack: () -> Unit,
    onToggleUpdateUser: () -> Unit,
    onSaveProfile: (name: String?, phone: String?, email: String?,) -> Unit,
    onProfilePictureRemoved: () -> Unit,
    onToggleNotifications: () -> Unit,
    onPolicyClick: () -> Unit,
    onDeleteAccount: () -> Unit,
    photoPickerLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>?,
    modifier: Modifier = Modifier,
) {
    var tempNewName by remember(state.currentUser.name) { mutableStateOf(state.currentUser.name) }
    var tempNewPhone by remember(state.currentUser.phone) { mutableStateOf(state.currentUser.phone) }
    var tempNewEmail by remember(state.currentUser.email) { mutableStateOf(state.currentUser.email) }

    Column(modifier.fillMaxWidth()
        .fillMaxHeight()
        .offset(x = 15.dp)) {
        Spacer(Modifier.padding(vertical = 20.dp))
        Image(
            painter = painterResource(R.drawable.arrow_left_green),
            contentDescription = "Back",
            modifier = Modifier
                //.align(Alignment.Center)
                .clickable(onClick = onBack)
                .size(50.dp)
                .offset(x = (12).dp, y = (13.dp)),
        )

        if (state.isUpdateUserOpen) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, 170),
                onDismissRequest = onToggleUpdateUser,
                properties = PopupProperties(focusable = true),
            ) {
                Box(
                    Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(15.dp))
                        .padding(12.dp)
                        .fillMaxWidth(0.75f)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f),
                        horizontalAlignment = Alignment.Start
                    ) {

                        Spacer(Modifier.height(25.dp))
                        Text("Name")
                        TextField(
                            value = tempNewName, onValueChange = { tempNewName = it },
                            placeholder = { Text("New Name") })

                        Spacer(Modifier.height(25.dp))
                        Text("Email")
                        TextField(
                            value = tempNewEmail, onValueChange = { tempNewEmail = it },
                            placeholder = { Text("New Email") })

                        Spacer(Modifier.height(25.dp))
                        Text("Phone Number")
                        TextField(
                            value = tempNewPhone, onValueChange = { tempNewPhone = it },
                            placeholder = { Text("New Phone") }, modifier = Modifier
                        )
                        Text("Profile Picture")
                        ProfilePicture(profileImageBitmap)
                        TextButton(onClick = {
                                photoPickerLauncher?.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                        }) {
                            Text(
                                "Choose new profile picture"
                            )
                        }
                        if(profileImageBitmap != null){
                            TextButton(onClick = {
                                onProfilePictureRemoved()
                            }) {
                                Text("Remove Image", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            Button(onClick = {
                                onSaveProfile(
                                    tempNewName,
                                    tempNewPhone,
                                    tempNewEmail
                                )
                            }, modifier = Modifier
                                .align(Alignment.BottomCenter)) {
                                Text(
                                    "Update", textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier.height(45.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                    )
                ) {

                    append("Account Details")
                }
            },

            style = MaterialTheme.typography.bodyLarge,
            fontSize = 40.sp,
            lineHeight = 48.sp,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .offset(y = 5.dp)
        )
        Text("Name")
        Text(state.currentUser.name)
        Text("Email")
        Text(state.currentUser.email)
        Text("Phone Number")
        Text(state.currentUser.phone)
        Text("Device ID")
        Text(state.currentUser.id)
        Text("Current Profile Picture")

        ProfilePicture(profileImageBitmap)



        Button(onClick = onToggleUpdateUser) { Text("Update Account Details") }

        Text("Notifications")
        Box(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Text(
                "Receive Notifications",
                modifier
                    .align(Alignment.CenterStart)
            )
            Switch(
                checked = state.currentUser.notificationsEnabled,
                onCheckedChange = { onToggleNotifications() },
                modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-50).dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {

            TextButton(
                onClick = onPolicyClick,
                modifier.align(Alignment.BottomStart)
            ) { Text("Policy and Guidelines") }
            Button(
                onClick = onDeleteAccount,
                modifier.align(Alignment.BottomEnd)
            ) { Text("Delete Account") }

        }
    }
}

@Composable
private fun ProfilePicture(
    profileImageBitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFD9D9D9)),
        contentAlignment = Alignment.Center,
    ) {
        if (profileImageBitmap != null) {
            Image(
                bitmap = profileImageBitmap,
                contentDescription = "Current profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Upload Image",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ".PNG, .JPEG, .IMG, .JPG",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
        }
    }
}

private val previewUser = User(
    id = "1234",
    name = "Test User",
    email = "test@example.com",
    phone = "780-111-1111",
    notificationsEnabled = true,
    profileImageId = "1"
)

@Preview(showBackground = true, showSystemUi = true, name = "Settings")
@Composable
private fun EntrantSettingsPreview() {
    CampsiteReservationAppTheme() {
        EntrantSettingsContent(
            state = EntrantSettingsState(
                currentUser = previewUser
            ),
            onBack = {}, onToggleUpdateUser = {}, onSaveProfile = { _, _, _ -> },
            onToggleNotifications = {}, onPolicyClick = {}, onDeleteAccount = {}, profileImageBitmap = null,
            photoPickerLauncher = null,
            onProfilePictureRemoved = {}

        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Update popup open")
@Composable
private fun EntrantSettingsPopupPreview() {
    CampsiteReservationAppTheme() {
        EntrantSettingsContent(
            state = EntrantSettingsState(
                currentUser = previewUser,
                isUpdateUserOpen = true
            ),
            onBack = {}, onToggleUpdateUser = {}, onSaveProfile = { _, _, _ -> },
            onToggleNotifications = {}, onPolicyClick = {}, onDeleteAccount = {}, profileImageBitmap = null,
            photoPickerLauncher = null,
            onProfilePictureRemoved = {}
        )
    }
}