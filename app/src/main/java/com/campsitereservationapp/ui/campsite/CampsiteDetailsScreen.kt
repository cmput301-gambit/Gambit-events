package com.campsitereservationapp.ui.campsite

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

/**
 * Used to represent the finite states of the main button at the bottom of the screen.
 */
enum class CampsiteDetailsMainButtonState {
    JOIN,
    LEAVE,
    UNAVAILABLE
}

/**
 * Screen used to fulfill US 01.01.05, 01.01.06, 01.02.01, 01.02.02, 01.13.01, and US 03.04.01 (View campsite details).
 *
 * **Purpose:** displays details for a campsite to entrants and administrators.
 *
 * **Design rationale:** displays campsite information and provides waitlist actions for entrants, or
 * an Admin View tag and deletion capability for administrators (US 03.04.01).
 *
 * **Outstanding issues:** none.
 *
 * @param navController navigation controller used for screen transitions
 * @param viewedCampsiteId ID of the campsite to display
 * @param isAdmin true if viewing as an administrator (US 03.04.01)
 * @param viewModel view model holding campsite details state
 */
@Composable
fun CampsiteDetailsScreen(
    navController: NavController,
    viewedCampsiteId: String,
    isAdmin: Boolean = false,
    viewModel: CampsiteDetailsViewModel = viewModel(factory = CampsiteDetailsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    Log.d("CampsiteDetailsScreen", "Starting details screen with viewedCampsiteId='$viewedCampsiteId'")
    LaunchedEffect(viewedCampsiteId) {
        viewModel.updateViewedCampSiteId(viewedCampsiteId)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()
            )
    ) {
        Spacer(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )
        Spacer(modifier = Modifier.padding(vertical = 8.dp))

        // Admin Tag (US 03.04.01)
        if (isAdmin) {
            Text(
                text = "Admin View",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
        }

        // Campsite Name
        Text(
            text = state.viewedCampsite.name,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.padding(vertical = 4.dp))

        // Location
        if (state.viewedCampsite.location.isNotBlank()) {
            Text("Location: ${state.viewedCampsite.location}")
        }

        // Capacity / Max Occupancy
        Text("Capacity: ${state.viewedCampsite.capacity}")

        // Description / Amenities
        if (state.viewedCampsite.description.isNotBlank()) {
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            Text("Description:\n${state.viewedCampsite.description}")
        }

        // Tags
        if (state.viewedCampsite.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            Text("Tags: ${state.viewedCampsite.tags.joinToString(", ")}")
        }

        // Map Coordinates (Latitude & Longitude)
        val lat = state.viewedCampsite.latitude
        val lon = state.viewedCampsite.longitude
        if (lat != null && lon != null) {
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            Text("Coordinates: $lat, $lon")
        }

        // Availability Date Range
        val availability = state.viewedCampsite.availability
        if (availability != null) {
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            Text("Available: ${availability.start} to ${availability.finish}")
        }

        Spacer(modifier = Modifier.padding(vertical = 8.dp))

        // Join / Leave Waitlist Button (Entrants) or Remove Campsite Button (Admin)
        if (isAdmin) {
            Button(
                onClick = {
                    viewModel.deleteCampsite(state.viewedCampsite.id)
                    navController.popBackStack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remove Campsite")
            }
        } else {
            Button(
                onClick = viewModel::updateButtonState
            ) {
                val txt = when (state.mainButtonState) {
                    CampsiteDetailsMainButtonState.JOIN -> "Join Waitlist"
                    CampsiteDetailsMainButtonState.LEAVE -> "Leave Waitlist"
                    CampsiteDetailsMainButtonState.UNAVAILABLE -> "Unavailable"
                }
                Text(txt)
            }
        }
    }
}
