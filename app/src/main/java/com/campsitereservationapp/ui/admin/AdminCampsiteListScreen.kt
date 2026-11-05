package com.campsitereservationapp.ui.admin

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Campsite

/**
 * Screen displaying the full list of campsites for admin management (US 03.04.01, 03.01.01).
 *
 * **Purpose:** allows administrators to view all campsites in the app, view details,
 * and remove any campsite after confirming in a popup.
 *
 * **Design rationale:** barebones card list displaying campsite summaries with "View More"
 * and "Remove" buttons bound to [AdminCampsiteListViewModel]. Removing asks for
 * confirmation first, since it also deletes the campsite's lotteries and can't be undone.
 *
 * **Outstanding issues:** none.
 *
 * @param navController navigation controller used for screen transitions
 * @param viewModel view model managing live campsite list state and removal
 */
@Composable
fun AdminCampsiteListScreen(
    navController: NavController,
    viewModel: AdminCampsiteListViewModel = viewModel(factory = AdminCampsiteListViewModel.Factory)
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
            text = "Campsite List",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.campsites.isEmpty() && !state.isLoading) {
            Text(
                text = "No campsites found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.campsites.forEach { campsite ->
                AdminCampsiteCard(
                    campsite = campsite,
                    onViewMore = {
                        navController.navigate("AdminCampsiteDetails/${campsite.id}")
                    },
                    onRemove = {
                        viewModel.requestRemoval(campsite)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // "Are you sure?" popup, shown while a removal is waiting for confirmation.
    state.campsitePendingRemoval?.let { campsite ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRemoval,
            title = { Text("Remove campsite?") },
            text = {
                Text("This will permanently delete ${campsite.name} and its lotteries. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRemoval) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRemoval) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * Card displaying a campsite summary with admin controls.
 *
 * @param campsite campsite data to display
 * @param onViewMore callback when "View More" is clicked
 * @param onRemove callback when "Remove" is clicked
 */
@Composable
private fun AdminCampsiteCard(
    campsite: Campsite,
    onViewMore: () -> Unit,
    onRemove: () -> Unit
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
                    onClick = onViewMore,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View More")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRemove,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Remove")
                }
            }
        }
    }
}