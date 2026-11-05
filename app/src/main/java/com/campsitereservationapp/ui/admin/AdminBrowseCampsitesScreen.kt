package com.campsitereservationapp.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Barebones screen for admin campsite browsing section (US 03.04.01).
 *
 * **Purpose:** provides options to view the list of all campsites or notifications.
 *
 * **Design rationale:** standard barebones Material 3 button list for navigation.
 *
 * **Outstanding issues:** "View All Notifications" is a placeholder for notification management, no styling has been applied.
 *
 * @param navController navigation controller used for screen transitions
 */
@Composable
fun AdminBrowseCampsitesScreen(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Browse Campsites",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("AdminCampsiteList") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View List of Campsites")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { /* Placeholder for future Notifications story */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View All Notifications")
        }
    }
}
