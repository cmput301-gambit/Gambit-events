package com.campsitereservationapp.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
 * Screen for the messages about one reservation (US 02.12.01, 01.14.01).
 *
 * **Purpose:** shows the conversation between a campsite owner and the
 * entrant who reserved, oldest first, with a box to send a new message.
 * Anyone who isn't part of the reservation sees why instead.
 *
 * **Outstanding issues:** plain styling.
 *
 * @param navController navigation controller used to go back
 * @param reservationId the reservation the conversation is about
 * @param viewModel view model holding the conversation state
 */
@Composable
fun ConversationScreen(
    navController: NavController,
    reservationId: String,
    viewModel: ConversationViewModel = viewModel(factory = ConversationViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(reservationId) { viewModel.load(reservationId) }
    val blockedReason = state.blockedReason

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Messages", style = MaterialTheme.typography.headlineMedium)

        when {
            state.isLoading -> Text("Loading...")
            blockedReason != null -> Text(blockedReason)
            else -> {
                Text("${state.otherPartyName} · ${state.campsiteName}")
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.messages.isEmpty()) {
                        item { Text("No messages yet.") }
                    }
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }
                state.error?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = viewModel::onDraftChanged,
                        label = { Text("Message") },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = viewModel::send, enabled = state.canSend) { Text("Send") }
                }
            }
        }
    }
}

/**
 * One message, lined up right if the user sent it and left if they received it.
 *
 * @param message the message to show
 */
@Composable
private fun MessageBubble(message: ConversationMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isMine) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(message.text)
                Text(message.sentAt, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}