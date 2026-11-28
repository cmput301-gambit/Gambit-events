package com.campsitereservationapp.ui.notifications

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.model.NotificationType

/**
 * Inbox screen where an entrant sees their lottery results (US 01.03.01, 01.03.02).
 *
 * **Purpose:** lists win and lose notifications in styled cards, newest first.
 * Unread ones are highlighted, and tapping one marks it as read and opens reservations if won.
 *
 * **Design rationale:** styled layout matching mockup design with top back arrow,
 * title, beige background, and grey notification log cards.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE6DA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = if (state.unreadCount > 0) "Notification Logs (${state.unreadCount} unread)" else "Notification Logs",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (state.notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notifications yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.notifications, key = { it.id }) { notification ->
                        NotificationCardItem(
                            notification = notification,
                            onClick = {
                                viewModel.onNotificationTapped(notification)
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
}

/**
 * Styled card displaying a single notification log item matching the mockup design.
 *
 * @param notification notification entity to display
 * @param onClick called when the card is tapped
 */
@Composable
private fun NotificationCardItem(notification: Notification, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFCCCCCC)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = notification.message,
                fontSize = 15.sp,
                fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold,
                color = Color.Black,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sent at ${notification.createdAt}",
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
    }
}
