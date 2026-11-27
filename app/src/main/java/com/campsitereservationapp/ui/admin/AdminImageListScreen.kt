package com.campsitereservationapp.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * Screen listing every uploaded image for the admin (US 03.06.01).
 *
 * **Purpose:** lets an admin browse all campsite posters and profile pictures
 * and see who uploaded each one.
 *
 * **Design rationale:** barebones card list matching [AdminRatingListScreen],
 * bound to [AdminImageListViewModel]. Uses a LazyColumn so only the pictures
 * on screen get decoded.
 *
 * **Outstanding issues:** no styling has been applied yet. There is no Remove
 * button; removing images is US 03.03.01.
 *
 * @param navController navigation controller used for screen transitions
 * @param viewModel view model providing the live image list
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
                AdminImageCard(row = row)
            }
        }
    }
}

/**
 * Card showing one uploaded image, what it's for, and who uploaded it.
 *
 * @param row the image and its uploader's name
 */
@Composable
private fun AdminImageCard(row: AdminImageRow) {
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

            Text(
                text = when (row.image.kind) {
                    ImageKind.CAMPSITE -> "Campsite poster"
                    ImageKind.PROFILE -> "Profile picture"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Uploaded by ${row.uploaderName ?: "unnamed user"} (${row.image.uploaderId})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
