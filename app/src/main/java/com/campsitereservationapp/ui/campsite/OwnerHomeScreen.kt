package com.campsitereservationapp.ui.campsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Home screen for campsite owners.
 *
 * **Purpose:** provides entry points for campsite owner management tasks,
 * specifically navigating to campsite registration.
 *
 * **Design rationale:** barebones layout using Material 3 text and buttons
 * to provide navigation options for campsite owners without styling bloat.
 *
 * **Outstanding issues:** "View My List of Campsites" action is intentionally omitted.
 *
 * @param navController navigation controller used for screen transitions
 */
@Composable
fun OwnerHomeScreen(
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
            text = "Welcome, Camp Owner",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("RegisterCampsite") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register Campsite")
        }
    }
}
