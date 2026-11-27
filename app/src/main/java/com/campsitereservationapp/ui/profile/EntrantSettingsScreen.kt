package com.campsitereservationapp.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.R
import com.campsitereservationapp.model.User
import com.campsitereservationapp.ui.theme.CampsiteReservationAppTheme

/**
 * Screen for managing user profile settings, notification toggles, and account details.
 *
 * @param navController navigation controller used for screen transitions
 * @param viewModel view model managing profile state and updates
 */
@Composable
fun EntrantSettingsScreen(
    navController: NavController,
    viewModel: EntrantSettingsViewModel = viewModel(factory = EntrantSettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    EntrantSettingsContent(
        state = state,
        onBack = { navController.popBackStack() },
        onToggleUpdateUser = viewModel::toggleUpdateUserOpen,
        onSaveProfile = { name, phone, email ->
            viewModel.updateUserDetails(
                newName = name,
                newPhone = phone,
                newEmail = email,
            )
            viewModel.toggleUpdateUserOpen()
        },
        onToggleNotifications = viewModel::toggleReceiveNotifications,
        onPolicyClick = { navController.navigate("PolicyAndGuidelines") },
        onDeleteAccount = viewModel::requestDeleteAccount,
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
    onBack: () -> Unit,
    onToggleUpdateUser: () -> Unit,
    onSaveProfile: (name: String, phone: String, email: String) -> Unit,
    onToggleNotifications: () -> Unit,
    onPolicyClick: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tempNewName by remember(state.currentUser.name) { mutableStateOf(state.currentUser.name) }
    var tempNewPhone by remember(state.currentUser.phone) { mutableStateOf(state.currentUser.phone) }
    var tempNewEmail by remember(state.currentUser.email) { mutableStateOf(state.currentUser.email) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEDE6DA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(20.dp))

            // Back Arrow Icon
            Image(
                painter = painterResource(R.drawable.arrow_left_green),
                contentDescription = "Back",
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onBack)
            )

            Spacer(Modifier.height(24.dp))

            // Account Details Title
            Text(
                text = "Account Details",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4C6B3E)
            )

            Spacer(Modifier.height(20.dp))

            // Name
            Text(
                text = "Name",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = if (state.currentUser.name.isNotBlank()) state.currentUser.name else "Not provided",
                fontSize = 18.sp,
                color = Color.DarkGray
            )

            Spacer(Modifier.height(18.dp))

            // Email
            Text(
                text = "Email",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = if (state.currentUser.email.isNotBlank()) state.currentUser.email else "Not provided",
                fontSize = 18.sp,
                color = Color.DarkGray
            )

            Spacer(Modifier.height(18.dp))

            // Phone Number
            Text(
                text = "Phone Number",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = if (state.currentUser.phone.isNotBlank()) state.currentUser.phone else "Not provided",
                fontSize = 18.sp,
                color = Color.DarkGray
            )

            Spacer(Modifier.height(18.dp))

            // Device ID
            Text(
                text = "Device ID",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = state.currentUser.id,
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(24.dp))

            // Update Account Details Button
            Button(
                onClick = onToggleUpdateUser,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4C6B3E),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Update Account Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(32.dp))

            // Notifications Section Title
            Text(
                text = "Notifications",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4C6B3E)
            )

            Spacer(Modifier.height(12.dp))

            // Receive Notifications Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Receive Notifications",
                    fontSize = 22.sp,
                    color = Color.Black
                )
                Switch(
                    checked = state.currentUser.notificationsEnabled,
                    onCheckedChange = { onToggleNotifications() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4C6B3E)
                    )
                )
            }

            Spacer(Modifier.height(40.dp))

            // Bottom Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Policy and guidelines",
                    fontSize = 15.sp,
                    color = Color.Black,
                    modifier = Modifier.clickable { onPolicyClick() }
                )

                Text(
                    text = "Delete Account",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE53935),
                    modifier = Modifier.clickable { onDeleteAccount() }
                )
            }
        }

        // Edit Profile Popup
        if (state.isUpdateUserOpen) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = onToggleUpdateUser,
                properties = PopupProperties(focusable = true),
            ) {
                Box(
                    Modifier
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                        .fillMaxWidth(0.85f)
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            "Update Account Details",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4C6B3E)
                        )

                        Spacer(Modifier.height(16.dp))

                        Text("Name", fontWeight = FontWeight.Bold)
                        TextField(
                            value = tempNewName,
                            onValueChange = { tempNewName = it },
                            placeholder = { Text("New Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Text("Email", fontWeight = FontWeight.Bold)
                        TextField(
                            value = tempNewEmail,
                            onValueChange = { tempNewEmail = it },
                            placeholder = { Text("New Email") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Text("Phone Number", fontWeight = FontWeight.Bold)
                        TextField(
                            value = tempNewPhone,
                            onValueChange = { tempNewPhone = it },
                            placeholder = { Text("New Phone") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = { onSaveProfile(tempNewName, tempNewPhone, tempNewEmail) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C6B3E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Update", textAlign = TextAlign.Center)
                        }
                    }
                }
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
)

@Preview(showBackground = true, showSystemUi = true, name = "Settings")
@Composable
private fun EntrantSettingsPreview() {
    CampsiteReservationAppTheme {
        EntrantSettingsContent(
            state = EntrantSettingsState(
                currentUser = previewUser
            ),
            onBack = {}, onToggleUpdateUser = {}, onSaveProfile = { _, _, _ -> },
            onToggleNotifications = {}, onPolicyClick = {}, onDeleteAccount = {},
        )
    }
}
