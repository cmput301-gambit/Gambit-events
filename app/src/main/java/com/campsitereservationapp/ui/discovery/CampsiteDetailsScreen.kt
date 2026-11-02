package com.campsitereservationapp.ui.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.campsitereservationapp.model.Campsite

/**
 * Used to represent the finite states of the main button at the bottom of the screen.
 */
enum class CampsiteDetailsMainButtonState{
    JOIN,
    LEAVE,
    UNAVAILABLE
}

/**
 * Screen used to fulfill US 01.01.05, 01.01.06, 01.02.01, 01.02.02, 01.13.01, and US 02.01.04 (View campsite details).
 */
@Composable
fun CampsiteDetailsScreen(
    navController: NavController,
    viewedCampsiteId : String,
    viewModel: CampsiteDetailsViewModel = viewModel(factory = CampsiteDetailsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewedCampsiteId) {
        viewModel.updateViewedCampSiteId(viewedCampsiteId)
    }

    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.padding(vertical = 16.dp))
        Text("Back",
            modifier = Modifier
                .clickable(onClick =
                    {
                        navController.popBackStack()
                    }
                )
        )
        Spacer(modifier = Modifier.padding(vertical = 8.dp))

        // Campsite Name
        Text(
            text = state.viewedCampsite.name,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.padding(vertical = 4.dp))

        // Owner Badge if viewing own campsite
        if (state.isOwner) {
            Text(
                text = "Owner View",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
        }

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

        // Join / Leave Waitlist Button (Entrants only, hidden for campsite owners)
        if (!state.isOwner) {
            Button(
                onClick = viewModel::updateButtonState
            ){
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
