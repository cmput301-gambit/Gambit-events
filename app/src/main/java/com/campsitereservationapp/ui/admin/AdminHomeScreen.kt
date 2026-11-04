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
 * Barebones admin welcome screen (US 03.04.01). (update comments when it is fully implemeneted)
 *
 * **Purpose:** provides entry points for admin management tasks, specifically browsing campsites.
 *
 * **Design rationale:** barebones Material 3 layout offering simple navigation buttons for admin tasks.
 *
 * **Outstanding issues:** "Browse Users" and "Browse All Photos" screens are placeholders for future stories, also there has been no styling applied.
 *
 * @param navController navigation controller used for screen transitions
 */
@Composable
fun AdminHomeScreen(
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
            text = "Welcome, Admin",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("AdminBrowseCampsites") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Browse Campsites")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { /* Placeholder for future Browse Users story */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Browse Users")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { /* Placeholder for future Browse All Photos story */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Browse All Photos")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Viewing as: Admin",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
