package com.campsitereservationapp.ui.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.format.DateTimeFormatter

/**
 * Screen where a campsite owner sees the notifications they sent to their
 * lotteries' waiting lists (US 02.10.02).
 *
 * **Purpose:** lists one row per send, newest first, with the message, the
 * lottery's campsites, how many entrants it went to and when it was sent.
 *
 * **Outstanding issues:** plain styling.
 *
 * @param navController navigation controller used to go back
 * @param viewModel view model holding the screen state
 */
@Composable
fun SentNotificationsScreen(
    navController: NavController,
    viewModel: SentNotificationsViewModel = viewModel(factory = SentNotificationsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

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
        Text("Sent Notifications", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> Text("Loading...")
            state.sends.isEmpty() -> Text("You haven't sent any notifications yet.")
            else -> LazyColumn {
                items(state.sends, key = { it.batchId }) { send -> SentRow(send) }
            }
        }
    }
}

private val sentAtFormat = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a")

/**
 * One sent message.
 *
 * @param send the send to show
 */
@Composable
private fun SentRow(send: SentNotification) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(send.campsiteNames, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(send.message)
        Spacer(modifier = Modifier.height(4.dp))
        val entrants = if (send.recipientCount == 1) "1 entrant" else "${send.recipientCount} entrants"
        Text(
            text = "Sent to $entrants · ${send.sentAt.format(sentAtFormat)}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    HorizontalDivider()
}
