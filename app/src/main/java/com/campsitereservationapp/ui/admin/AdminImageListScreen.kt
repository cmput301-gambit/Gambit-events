package com.campsitereservationapp.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.ImageKind
import com.campsitereservationapp.ui.components.rememberImageBitmap

/**
 * Screen listing every uploaded image for the admin (US 03.06.01, 03.03.01).
 *
 * **Purpose:** lets an admin browse all campsite posters and profile pictures,
 * see who uploaded each one, and remove ones that break the app's rules
 * after confirming in a popup.
 *
 * **Design rationale:** barebones card list matching [AdminRatingListScreen],
 * bound to [AdminImageListViewModel]. Uses a LazyColumn so only the pictures
 * on screen get decoded.
 *
 * **Outstanding issues:** no styling has been applied yet.
 *
 * @param navController navigation controller used for screen transitions
 * @param viewModel view model providing the live image list and removals
 */
@Composable
fun AdminImageListScreen(
    navController: NavController,
    viewModel: AdminImageListViewModel = viewModel(factory = AdminImageListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "Back",
                    modifier = Modifier.clickable { navController.popBackStack() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Photos",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        if (state.images.isEmpty() && !state.isLoading) {
            item {
                Text(
                    text = "No photos uploaded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.images, key = { it.image.id }) { row ->
                AdminImageCard(row = row, onRemove = { viewModel.requestRemoval(row) })
            }
        }
    }

    // "Are you sure?" popup, shown while a removal is waiting for confirmation.
    state.imagePendingRemoval?.let { row ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRemoval,
            title = { Text("Remove photo?") },
            text = {
                Text(
                    "This will permanently delete this ${kindLabel(row.image.kind).lowercase()} " +
                        "uploaded by ${row.uploaderName ?: "an unnamed user"}. This can't be undone."
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
 * Card showing one uploaded image, what it's for, who uploaded it, and a Remove button.
 *
 * @param row the image and its uploader's name
 * @param onRemove callback when "Remove" is clicked
 */
@Composable
private fun AdminImageCard(
    row: AdminImageRow,
    onRemove: () -> Unit,
) {
    val bitmap = rememberImageBitmap(row.image.data)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Uploaded photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = "Can't show this photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = kindLabel(row.image.kind),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Uploaded by ${row.uploaderName ?: "unnamed user"} (${row.image.uploaderId})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRemove,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Remove")
                }
            }
        }
    }
}

// What the picture is used for, shown on the card and in the popup.
private fun kindLabel(kind: ImageKind): String = when (kind) {
    ImageKind.CAMPSITE -> "Campsite poster"
    ImageKind.PROFILE -> "Profile picture"
}
