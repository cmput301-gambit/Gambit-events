package com.campsitereservationapp.ui.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.model.NotificationType

/**
 * Inbox screen where an entrant sees their lottery results (US 01.03.01, 01.03.02).
 *
 * **Purpose:** lists the user's win and lose notifications, newest first.
 * Unread ones are bold, and tapping one marks it as read. Tapping a win
 * also opens the reserve screen, so the winner can pick their dates
 * (US 01.04.01, 01.05.01).
 *
 * **Outstanding issues:** plain styling for now, and no loading state, so
 * "No notifications yet" shows briefly before the data arrives.
 *
 * @param navController navigation controller used to go back and to open the reserve screen
 * @param viewModel view model holding the inbox state
 */
@Composable
fun InboxScreen(
    navController: NavController,
    viewModel: InboxViewModel = viewModel(factory = InboxViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.padding(16.dp)) {
        Spacer(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )
        Spacer(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = if (state.unreadCount > 0) "Inbox (${state.unreadCount} unread)" else "Inbox",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.padding(vertical = 8.dp))

        if (state.notifications.isEmpty()) {
            Text("No notifications yet. Your lottery results will show up here.")
        } else {
            LazyColumn {
                items(state.notifications, key = { it.id }) { notification ->
                    NotificationRow(
                        notification = notification,
                        onClick = {
                            viewModel.onNotificationTapped(notification)
                            // Tapping a win takes the winner to reserve their dates.
                            if (notification.type == NotificationType.LOTTERY_WON) {
                                navController.navigate("Reserve/${notification.lotteryId}")
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * One notification in the inbox. Bold when unread.
 *
 * @param notification the notification to show
 * @param onClick called when the user taps it
 */
@Composable
private fun NotificationRow(notification: Notification, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = notification.message,
            fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold,
        )
        Text(
            text = notification.createdAt.toString(),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    HorizontalDivider()
}