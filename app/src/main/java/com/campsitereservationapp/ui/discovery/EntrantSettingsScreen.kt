package com.campsitereservationapp.ui.discovery

import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
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
 * Screen coinciding with M11
 *
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
        onDeleteAccount = { /* TODO */ },
    )
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

    Column(modifier.fillMaxWidth()
        .fillMaxHeight()
        .offset(x = 15.dp)) {
        Spacer(Modifier.padding(vertical = 20.dp))
        Image(
            painter = painterResource(R.drawable.arrow_left_green),
            contentDescription = "To settings icon",
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
                    Column(Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f),
                        horizontalAlignment = Alignment.Start) {

                        Spacer(Modifier.height(25.dp))
                        Text("Name")
                        TextField(value = tempNewName, onValueChange = { tempNewName = it },
                            placeholder = { Text("New Name") })

                        Spacer(Modifier.height(25.dp))
                        Text("Email")
                        TextField(value = tempNewEmail, onValueChange = { tempNewEmail = it },
                            placeholder = { Text("New Email") })

                        Spacer(Modifier.height(25.dp))
                        Text("Phone Number")
                        TextField(value = tempNewPhone, onValueChange = { tempNewPhone = it },
                            placeholder = { Text("New Phone") }, modifier = Modifier)
                        Box(modifier = Modifier
                            .fillMaxSize()){
                            Button(onClick = { onSaveProfile(tempNewName, tempNewPhone, tempNewEmail) }
                            , modifier = Modifier
                                    .align(Alignment.BottomCenter)) {
                                Text("Update", textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier.height(45.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold
                    , color = MaterialTheme.colorScheme.primary)){

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

        Button(onClick = onToggleUpdateUser) { Text("Update Account Details") }

        Text("Notifications")
        Box(modifier = Modifier
            .fillMaxWidth(),
            ) {
            Text("Receive Notifications",
                modifier
                    .align(Alignment.CenterStart))
            Switch(checked = state.currentUser.notificationsEnabled,
                onCheckedChange = { onToggleNotifications() },
                modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-50).dp)
            )
        }
        Box(modifier = Modifier
            .fillMaxSize()){

                TextButton(onClick = onPolicyClick,
                    modifier.align(Alignment.BottomStart)) { Text("Policy and Guidelines") }
                Button(onClick = onDeleteAccount,
                    modifier.align(Alignment.BottomEnd)) { Text("Delete Account") }

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
    CampsiteReservationAppTheme() {
        EntrantSettingsContent(
            state = EntrantSettingsState(currentUser = previewUser),
            onBack = {}, onToggleUpdateUser = {}, onSaveProfile = { _, _, _ -> },
            onToggleNotifications = {}, onPolicyClick = {}, onDeleteAccount = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Update popup open")
@Composable
private fun EntrantSettingsPopupPreview() {
    CampsiteReservationAppTheme() {
        EntrantSettingsContent(
            state = EntrantSettingsState(currentUser = previewUser, isUpdateUserOpen = true),
            onBack = {}, onToggleUpdateUser = {}, onSaveProfile = { _, _, _ -> },
            onToggleNotifications = {}, onPolicyClick = {}, onDeleteAccount = {},
        )
    }
}