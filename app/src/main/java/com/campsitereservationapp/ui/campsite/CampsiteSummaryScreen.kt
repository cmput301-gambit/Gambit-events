package com.campsitereservationapp.ui.campsite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campsitereservationapp.ui.components.CampsiteCard
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row  // you have this one
import androidx.compose.ui.tooling.preview.Preview
import com.campsitereservationapp.ui.theme.CampsiteReservationAppTheme


/**
 * Campsite summary
 * CampsiteSummaryCard: The original list card where entrants are able to view
 * the summary of the campsite
 *
 *CampsitrFullScreen: Entrants are able to see the full view of the campsite
 * information
 */

@Composable
fun CampsiteSummary(
    name: String,
    location: String,
    rating: Double,
    description: String,
    capacity: Int
) {


}

@Composable
fun CampsiteFullScreen (
    name: String,
    location: String,
    rating: Double,
    description: String,
    capacity: Int,
    tags: List<String>,
    lotteryStatus: String,
    lotteryDates: String,
    onBack: () -> Unit,
    onEditLottery: () -> Unit,
    onViewRegistration: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.tertiary)
        )

        Spacer(Modifier.height(12.dp))

        Row {
           repeat(rating.toInt()) {
               Icon(
               Icons.Filled.Star,
               contentDescription = null,
               tint = MaterialTheme.colorScheme.secondary,
                   modifier = Modifier.size(36.dp)
               )
           }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { tag ->
                Text(
                    text = tag,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.onSecondary, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp)

                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(location, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium)
        Text("Capacity: $capacity", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Row {
            Text("ACTIVE LOTTERY: ", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Text(lotteryDates, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))
        Text(description, color = MaterialTheme.colorScheme.onPrimary)
        Spacer(Modifier.height(24.dp))


    }
}