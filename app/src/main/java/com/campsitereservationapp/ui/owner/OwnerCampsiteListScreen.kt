package com.campsitereservationapp.ui.owner

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Campsite

/**
 * Screen listing all campsites registered by the owner (US 02.03.01).
 *
 * **Purpose:** provides campsite owners a centralized list of their registered campsites,
 * offering options to view details or edit each campsite.
 *
 * **Design rationale:** barebones card list displaying campsite name, location, and capacity
 * with "Edit Camp" and "View More" actions.
 *
 * **Outstanding issues:** Needs major styling to make it look like our mockups.
 *
 * @param navController navigation controller used for screen transitions
 * @param viewModel view model holding live owner campsite list state
 */
@Composable
fun OwnerCampsiteListScreen(
    navController: NavController,
    viewModel: OwnerCampsiteListViewModel = viewModel(factory = OwnerCampsiteListViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()

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
            text = "My Campsites",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.campsites.isEmpty() && !state.isLoading) {
            Text(
                text = "You haven't registered any campsites yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.campsites.forEach { campsite ->
                OwnerCampsiteCard(
                    campsite = campsite,
                    onEditCamp = {
                        navController.navigate("EditCampsite/${campsite.id}")
                    },
                    onViewMore = {
                        navController.navigate("OwnerCampsiteDetails/${campsite.id}")
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Card displaying an owner's campsite summary with edit and view options.
 *
 * @param campsite campsite data to display
 * @param onEditCamp callback when "Edit Camp" is clicked
 * @param onViewMore callback when "View More" is clicked
 */
@Composable
private fun OwnerCampsiteCard(
    campsite: Campsite,
    onEditCamp: () -> Unit,
    onViewMore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = campsite.name,
                style = MaterialTheme.typography.titleMedium
            )

            if (campsite.location.isNotBlank()) {
                Text(
                    text = "Location: ${campsite.location}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Capacity: ${campsite.capacity}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onEditCamp,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit Camp")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onViewMore,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View More")
                }
            }
        }
    }
}
