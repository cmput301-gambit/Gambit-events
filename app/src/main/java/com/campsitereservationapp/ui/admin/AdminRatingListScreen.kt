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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.ui.components.StarRow
import com.campsitereservationapp.ui.components.formatScore

/**
 * Screen listing every campsite rating and its written review for admin
 * management (US 03.09.01, 01.13.02).
 *
 * **Purpose:** lets an admin read ratings and reviews and remove ones that
 * break the app's rules, after confirming in a popup.
 *
 * **Design rationale:** card list matching [AdminCampsiteListScreen], bound to
 * [AdminRatingListViewModel]. A review is part of its rating, so the review
 * text shows on the rating's card and Remove deletes both.
 *
 * **Outstanding issues:** None.
 *
 * @param navController navigation controller used for screen transitions
 * @param viewModel view model managing the live rating list and removals
 */
@Composable
fun AdminRatingListScreen(
    navController: NavController,
    viewModel: AdminRatingListViewModel = viewModel(factory = AdminRatingListViewModel.Factory),
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
            text = "Ratings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.ratings.isEmpty() && !state.isLoading) {
            Text(
                text = "No ratings found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.ratings.forEach { row ->
                AdminRatingCard(row = row, onRemove = { viewModel.requestRemoval(row) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // "Are you sure?" popup, shown while a removal is waiting for confirmation.
    state.ratingPendingRemoval?.let { row ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRemoval,
            title = { Text(if (row.rating.hasReview) "Remove rating and review?" else "Remove rating?") },
            text = {
                Text(
                    "This will permanently delete this ${formatScore(row.rating.score)}-star " +
                        (if (row.rating.hasReview) "rating and its review" else "rating") +
                        " for ${row.campsiteName}. This can't be undone."
                )
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
 * Card showing one rating, its written review if any, who left it, and a
 * Remove button (US 03.09.01, 01.13.02).
 *
 * @param row the rating and its campsite name
 * @param onRemove callback when "Remove" is clicked
 */
@Composable
private fun AdminRatingCard(
    row: AdminRatingRow,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = row.campsiteName, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StarRow(score = row.rating.score, starSize = 18.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${formatScore(row.rating.score)} / 5",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRemove,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Remove")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (row.rating.hasReview) {
                Text(
                    text = "\u201C${row.rating.review}\u201D",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "No written review",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "By ${row.raterName ?: "unnamed user"}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = row.rating.userId,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
