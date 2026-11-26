package com.campsitereservationapp.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

/**
 * Screen where a campsite owner picks a guest to message (US 02.12.01).
 *
 * **Purpose:** lists the entrants with an active reservation at the
 * owner's campsite, each with a Message button that opens the conversation.
 * Anyone who isn't the campsite's owner sees a message instead of the list.
 *
 * **Outstanding issues:** plain styling.
 *
 * @param navController navigation controller used to go back and open a conversation
 * @param campsiteId the owner's campsite
 * @param viewModel view model holding the guest list
 */
@Composable
fun OwnerGuestsScreen(
    navController: NavController,
    campsiteId: String,
    viewModel: OwnerGuestsViewModel = viewModel(factory = OwnerGuestsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    val blockedReason = state.blockedReason
    LaunchedEffect(campsiteId) { viewModel.load(campsiteId) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Message guests", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> Text("Loading...")
            blockedReason != null -> Text(blockedReason)
            state.guests.isEmpty() -> Text("No one has an upcoming reservation at this campsite yet.")
            else -> state.guests.forEach { guest ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(guest.guestName, style = MaterialTheme.typography.titleMedium)
                            Text(guest.dates)
                        }
                        Button(onClick = { navController.navigate("Conversation/${guest.reservationId}") }) {
                            Text("Message")
                        }
                    }
                }
            }
        }
    }
}