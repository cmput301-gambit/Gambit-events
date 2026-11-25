@file:Suppress("NewApi")

package com.campsitereservationapp.ui.campsite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.LocalDate

/**
 * Screen displaying currently booked dates for a campsite (US 02.06.01).
 *
 * **Purpose:** allows campsite owners to view taken calendar dates and active
 * reservations for their campsite.
 *
 * **Design rationale:** displays a calendar grid highlighting booked days of the month,
 * followed by cards listing entrant names and booked date ranges.
 *
 * **Outstanding issues:** none.
 *
 * @param navController navigation controller used to return to previous screen
 * @param campsiteId ID of the campsite whose booked dates to view
 * @param viewModel view model providing booked dates state
 */
@Composable
fun BookedDatesScreen(
    navController: NavController,
    campsiteId: String,
    viewModel: BookedDatesViewModel = viewModel(factory = BookedDatesViewModel.provideFactory(campsiteId))
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
            text = "Booked Dates",
            style = MaterialTheme.typography.headlineMedium
        )

        if (state.campsiteName.isNotBlank()) {
            Text(
                text = state.campsiteName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Card
        MonthCalendarCard(
            monthName = if (state.monthName.isNotBlank()) state.monthName else "Month",
            bookedDays = state.bookedDaysOfMonth
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Booked Entries List
        if (state.bookedDatesList.isEmpty() && !state.isLoading) {
            Text(
                text = "No active bookings for this campsite.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.bookedDatesList.forEach { item ->
                BookedDateEntryCard(item = item)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

/**
 * Calendar visual displaying days of the month with booked days highlighted.
 *
 * @param monthName display name of the month
 * @param bookedDays set of day-of-month integers that are booked
 */
@Composable
private fun MonthCalendarCard(
    monthName: String,
    bookedDays: Set<Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Day headers: S M T W T F S
            val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid of days 1..28 (or current month length)
            val today = LocalDate.now()
            val daysInMonth = today.lengthOfMonth()
            val daysList = (1..daysInMonth).toList()

            val rows = daysList.chunked(7)
            rows.forEach { rowDays ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rowDays.forEach { day ->
                        val isBooked = day in bookedDays
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isBooked) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                fontSize = 13.sp,
                                color = if (isBooked) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isBooked) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    // Fill remaining slots in last row if fewer than 7
                    if (rowDays.size < 7) {
                        repeat(7 - rowDays.size) {
                            Spacer(modifier = Modifier.size(32.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Entry card displaying a single booking's entrant name and date range.
 *
 * @param item booked date item to display
 */
@Composable
private fun BookedDateEntryCard(
    item: BookedDateItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.entrantName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = item.entrantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = item.dateRangeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}
