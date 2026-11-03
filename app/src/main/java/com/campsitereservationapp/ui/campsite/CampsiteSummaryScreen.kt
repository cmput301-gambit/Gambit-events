package com.campsitereservationapp.ui.campsite

import android.media.Rating
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.ui.components.CampsiteCard
import java.nio.file.WatchEvent
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton


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

        }

    }
}