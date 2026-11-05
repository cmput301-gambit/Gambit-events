package com.campsitereservationapp.ui.campsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.ui.discovery.CampsiteDetailsViewModel

/**
 * Screen displaying full details of an owner's campsite (US 02.01.04).
 *
 * **Purpose:** allows a campsite owner to view all details entered for their campsite,
 * offering quick navigation placeholders for lottery management and viewing reservations.
 *
 * **Design rationale:** displays owner view badge, complete property details, and
 * owner action buttons ("Edit Lottery" and "View Reservations" placeholders).
 *
 * **Outstanding issues:** "Edit Lottery" and "View Reservations" actions are placeholders for lottery/reservation stories.
 * Also, there is no image display for the campsite. Major styling required as well.
 *
 * @param navController navigation controller used for screen transitions
 * @param campsiteId ID of the campsite to display
 * @param viewModel view model providing campsite details state
 */
@Composable
fun OwnerCampsiteDetailsScreen(
    navController: NavController,
    campsiteId: String,
    viewModel: CampsiteDetailsViewModel = viewModel(factory = CampsiteDetailsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(campsiteId) {
        viewModel.updateViewedCampSiteId(campsiteId)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Owner View",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = state.viewedCampsite.name,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.viewedCampsite.location.isNotBlank()) {
            Text("Location: ${state.viewedCampsite.location}")
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text("Capacity: ${state.viewedCampsite.capacity}")

        if (state.viewedCampsite.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Description (Amenities):\n${state.viewedCampsite.description}")
        }

        if (state.viewedCampsite.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tags: ${state.viewedCampsite.tags.joinToString(", ")}")
        }

        val lat = state.viewedCampsite.latitude
        val lon = state.viewedCampsite.longitude
        if (lat != null && lon != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Coordinates: $lat, $lon")
        }

        val availability = state.viewedCampsite.availability
        if (availability != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Available: ${availability.start} to ${availability.finish}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { /* Placeholder for Edit Lottery story */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Edit Lottery")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { /* Placeholder for View Reservations story */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("View Reservations")
            }
        }
    }
}
