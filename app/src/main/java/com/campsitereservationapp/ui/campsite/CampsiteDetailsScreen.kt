package com.campsitereservationapp.ui.campsite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Rating
import com.campsitereservationapp.ui.components.StarPicker
import com.campsitereservationapp.ui.components.StarRow
import com.campsitereservationapp.ui.components.formatScore
import com.campsitereservationapp.ui.theme.CampTertiary

/**
 * Used to represent the finite states of the main button at the bottom of the screen.
 */
enum class CampsiteDetailsMainButtonState {
    JOIN,
    LEAVE,
    UNAVAILABLE
}

/**
 * Screen used to fulfill US 01.01.05, 01.01.06, 01.02.01, 01.02.02, 01.13.01, 01.13.02, and US 03.04.01 (View campsite details).
 *
 * **Purpose:** displays details for a campsite to entrants and administrators, including its
 * star score and the reviews other campers wrote.
 *
 * **Design rationale:** displays campsite information and provides waitlist actions for entrants, or
 * an Admin View tag and deletion capability for administrators (US 03.04.01). Rating works like a
 * product review: tap the stars, then optionally write a few words before submitting (US 01.13.02).
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

    LaunchedEffect(viewedCampsiteId) {
        viewModel.updateViewedCampSiteId(viewedCampsiteId)
    }

    var givenScore by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

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
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarRow(score = state.viewedCampsiteScore)
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.ratingCount == 0) "No ratings yet"
                else "${formatScore(state.viewedCampsiteScore)} (${state.ratingCount} " +
                    (if (state.ratingCount == 1) "rating)" else "ratings)"),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

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

        // Reviews other campers wrote (US 01.13.02)
        Text("Reviews", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (state.reviews.isEmpty()) {
            Text(
                "No written reviews yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.reviews.forEach { review ->
                ReviewCard(review, onDelete = { confirmDelete = true })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (!isAdmin && !state.hasGivenRating) {
            OutlinedButton(
                onClick = {
                    givenScore = 0
                    reviewText = ""
                    viewModel.toggleRatingPopup()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Rate and review")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Delete your review?") },
                text = { Text("Your stars and review for this campsite will be removed. You can rate it again afterwards.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        viewModel.deleteMyRating()
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                },
            )
        }

        if (state.giveRatingPopupOpen) {
            RateDialog(
                campsiteName = state.viewedCampsite.name,
                score = givenScore,
                onScoreChange = { givenScore = it },
                review = reviewText,
                onReviewChange = { reviewText = it.take(Rating.MAX_REVIEW_LENGTH) },
                onSubmit = { viewModel.onCreateRating(givenScore.toFloat(), reviewText) },
                onDismiss = viewModel::toggleRatingPopup,
            )
        }

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

/**
 * One camper's written review: name, stars and text (US 01.13.02). The
 * user's own review also gets a Delete button.
 *
 * @param review the review to show
 * @param onDelete called when the user taps Delete on their own review
 */
@Composable
private fun ReviewCard(review: CampsiteReview, onDelete: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CampTertiary.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (review.isMine) "${review.authorName} (you)" else review.authorName,
                fontWeight = FontWeight.SemiBold,
            )
            StarRow(score = review.rating.score, starSize = 16.dp)
        }
        Spacer(Modifier.height(4.dp))
        if (review.rating.hasReview) {
            Text(review.rating.review, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                "You rated this campsite without a written review.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (review.isMine) {
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * Popup for rating a campsite: tap the stars, then optionally write a review
 * before submitting, like a product review (US 01.13.01, 01.13.02).
 *
 * @param campsiteName shown in the title
 * @param score the picked star count, or 0 if none yet
 * @param onScoreChange called when a star is tapped
 * @param review the review typed so far
 * @param onReviewChange called as the review is typed
 * @param onSubmit saves the rating and review
 * @param onDismiss closes the popup without saving
 */
@Composable
private fun RateDialog(
    campsiteName: String,
    score: Int,
    onScoreChange: (Int) -> Unit,
    review: String,
    onReviewChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (campsiteName.isBlank()) "Rate this campsite" else "Rate $campsiteName") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StarPicker(score = score, onScoreChange = onScoreChange)
                Text(
                    when (score) {
                        0 -> "Tap a star to rate"
                        1 -> "Poor"
                        2 -> "Fair"
                        3 -> "Good"
                        4 -> "Very good"
                        else -> "Excellent"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = review,
                    onValueChange = onReviewChange,
                    label = { Text("Write a review (optional)") },
                    placeholder = { Text("What did you like or dislike?") },
                    supportingText = { Text("${review.length}/${Rating.MAX_REVIEW_LENGTH}") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = score > 0) {
                Text(if (review.isBlank()) "Submit rating" else "Submit review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
