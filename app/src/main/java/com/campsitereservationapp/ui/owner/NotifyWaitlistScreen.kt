package com.campsitereservationapp.ui.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Notification

/**
 * Screen where a campsite owner sends a notification to everyone on one
 * lottery's waiting list (US 02.10.01).
 *
 * **Purpose:** shows the lottery's campsites and how many entrants are
 * waiting, a box for the message with a character counter, and the result
 * of the send. Anyone who doesn't own the lottery sees why instead.
 *
 * **Outstanding issues:** plain styling.
 *
 * @param navController navigation controller used to go back
 * @param lotteryId the lottery whose waiting list is messaged
 * @param viewModel view model holding the screen state
 */
@Composable
fun NotifyWaitlistScreen(
    navController: NavController,
    lotteryId: String,
    viewModel: NotifyWaitlistViewModel = viewModel(factory = NotifyWaitlistViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(lotteryId) { viewModel.load(lotteryId) }
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
        Text("Notify Waitlist", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        when {
            state.isLoading -> Text("Loading...")
            blockedReason != null -> Text(blockedReason)
            else -> {
                Text(state.campsiteNames, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${state.waitlistCount} on the waitlist",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = viewModel::onDraftChanged,
                    enabled = state.canSend,
                    placeholder = { Text("Write a message to everyone on the waitlist") },
                    supportingText = {
                        Text("${state.draft.trim().length}/${Notification.MAX_LENGTH}")
                    },
                    isError = state.draft.trim().length > Notification.MAX_LENGTH,
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                state.result?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = viewModel::send,
                    enabled = state.canSend,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isSending) "Sending..." else "Send")
                }
            }
        }
    }
}
