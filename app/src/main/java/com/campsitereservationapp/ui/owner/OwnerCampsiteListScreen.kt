package com.campsitereservationapp.ui.owner

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.R
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.ui.campsite.rememberBitmapFromUri

/**
 * Screen listing all campsites registered by the owner matching the design mockup (US 02.03.01).
 *
 * **Purpose:** provides campsite owners a centralized list of their registered campsites and styled campsite cards.
 *
 * **Design rationale:** displays top header with back arrow, logo, and notification icon, and dark green
 * campsite cards with image thumbnail, status badge, "Edit Camp", and "View More" action buttons.
 *
 * **Outstanding issues:** rating stars excluded per specification.
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
            .fillMaxSize()
            .background(Color(0xFFEDE6DA))
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Top Header Bar with Back Arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { navController.popBackStack() }
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(28.dp),
                        tint = Color(0xFF2C3E2B)
                    )
                }
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2C3E2B)
                )
            }

            Image(
                painter = painterResource(R.drawable.primary_logo),
                contentDescription = "App Logo",
                modifier = Modifier.height(48.dp)
            )

            IconButton(onClick = { navController.navigate("Inbox") }) {
                Image(
                    painter = painterResource(R.drawable.notification_icon),
                    contentDescription = "Notifications",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "My Campsites",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E2B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campsite Card List
        if (state.campsites.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You haven't registered any campsites yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.campsites, key = { it.id }) { campsite ->
                    OwnerCampsiteCardItem(
                        campsite = campsite,
                        onEditCamp = {
                            navController.navigate("EditCampsite/${campsite.id}")
                        },
                        onViewMore = {
                            navController.navigate("OwnerCampsiteDetails/${campsite.id}")
                        }
                    )
                }
            }
        }
    }
}

/**
 * Custom styled owner campsite card matching the mockup design.
 *
 * @param campsite campsite entity to render
 * @param onEditCamp callback when "Edit Camp" is tapped
 * @param onViewMore callback when "View More" is tapped
 */
@Composable
private fun OwnerCampsiteCardItem(
    campsite: Campsite,
    onEditCamp: () -> Unit,
    onViewMore: () -> Unit
) {
    val imageUri = campsite.posterImageId?.let { Uri.parse(it) }
    val imageBitmap = rememberBitmapFromUri(imageUri)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4C6B3E)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Campsite Image / Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF8A9A86)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = campsite.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = campsite.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Details and Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE LOTTERY",
                        color = Color(0xFF70D6FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = campsite.name,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onEditCamp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFCCCCCC),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Edit Camp",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onViewMore,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF90E0EF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "View More",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
