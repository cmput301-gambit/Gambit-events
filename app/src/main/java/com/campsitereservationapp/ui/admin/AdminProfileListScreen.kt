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
import com.campsitereservationapp.model.User

/**
 * Screen listing every profile for admin management (US 03.02.01, 03.07.01).
 *
 * **Purpose:** lets an admin remove any profile, or remove a campsite owner
 * who broke the rules, after confirming in a popup.
 *
 * **Design rationale:** barebones card list matching [AdminCampsiteListScreen],
 * bound to [AdminProfileListViewModel]. "Remove as owner" only appears on
 * owners' cards. The admin's own card has no Remove buttons.
 *
 * **Outstanding issues:** no styling has been applied yet.
 *
 * @param navController navigation controller used for screen transitions
 * @param viewModel view model managing the live profile list and removals
 */
@Composable
fun AdminProfileListScreen(
    navController: NavController,
    viewModel: AdminProfileListViewModel = viewModel(factory = AdminProfileListViewModel.Factory),
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
            text = "Profiles",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.users.isEmpty() && !state.isLoading) {
            Text(
                text = "No profiles found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.users.forEach { user ->
                AdminProfileCard(
                    user = user,
                    isCurrentUser = user.id == state.currentUserId,
                    onRemoveProfile = { viewModel.requestProfileRemoval(user) },
                    onRemoveOwner = { viewModel.requestOwnerRemoval(user) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // "Are you sure?" popup, shown while a removal is waiting for confirmation.
    state.pendingRemoval?.let { removal ->
        val name = AdminProfileListViewModel.displayName(removal.user)
        val (title, message) = when (removal) {
            is ProfileRemoval.WholeProfile -> "Remove profile?" to
                "This will permanently delete $name's profile, along with their waiting list " +
                "spots, reservations, messages, notifications, ratings, and any campsites " +
                "and lotteries they own. This can't be undone."
            is ProfileRemoval.OwnerRole -> "Remove campsite owner?" to
                "This will permanently delete every campsite and lottery $name owns and " +
                "stop them from being an owner. Their entrant profile is kept. This can't be undone."
        }
        AlertDialog(
            onDismissRequest = viewModel::cancelRemoval,
            title = { Text(title) },
            text = { Text(message) },
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

    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Something went wrong") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) { Text("OK") }
            },
        )
    }
}

/**
 * Card showing one profile with admin controls.
 *
 * @param user the profile to display
 * @param isCurrentUser true for the admin's own card, which has no Remove buttons
 * @param onRemoveProfile callback when "Remove profile" is clicked
 * @param onRemoveOwner callback when "Remove as owner" is clicked
 */
@Composable
private fun AdminProfileCard(
    user: User,
    isCurrentUser: Boolean,
    onRemoveProfile: () -> Unit,
    onRemoveOwner: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = AdminProfileListViewModel.displayName(user) + if (isCurrentUser) " (you)" else "",
                style = MaterialTheme.typography.titleMedium
            )

            val roles = listOfNotNull(
                "Entrant".takeIf { user.isEntrant },
                "Owner".takeIf { user.isOwner },
                "Admin".takeIf { user.isAdmin },
            )
            Text(
                text = "Roles: " + roles.ifEmpty { listOf("None") }.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium
            )

            if (user.email.isNotBlank()) {
                Text(text = user.email, style = MaterialTheme.typography.bodySmall)
            }

            if (!isCurrentUser) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    if (user.isOwner) {
                        OutlinedButton(
                            onClick = onRemoveOwner,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Remove as owner")
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = onRemoveProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove profile")
                    }
                }
            }
        }
    }
}
