package com.campsitereservationapp.ui.reservations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campsitereservationapp.ui.theme.CampsiteReservationAppTheme
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.ui.campsite.ReservationListItem
import com.campsitereservationapp.ui.campsite.ReservationListUiState
import com.campsitereservationapp.ui.campsite.ReservationListViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Reservation List Screen (US 01.06.01, 01.07.01)
 *
 * Purpose: Show's the reservations where they're able to cancel and confirm cancellation.
 * Back button will return to home screen.
 *
 * Design Rationale: Reliant on components from the theme that's reused so screen matches the
 * Figma.
 *
 * **Outstanding issues:** past and cancelled reservations aren't shown
 * anywhere yet (US 01.06.02).
 *
 * @param navController used by the back button to return to the home screen
 * @param viewModel provides the entrant's reservations and cancels them
 */

@Composable
fun ReservationListScreen(
    navController: NavController,
    viewModel: ReservationListViewModel = viewModel(factory = ReservationListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    ReservationListContent(
        state = state,
        onBack = { navController.popBackStack() },
        onCancel = viewModel::cancel,
    )
}

/**
 * Draws the reservation list from [state], with no ViewModel, so it can be
 * previewed without Firebase.
 *
 * @param state the reservations to show
 * @param onBack called when the back button is tapped
 * @param onCancel called with a reservation's ID once the entrant confirms cancelling
 */
@Composable
fun ReservationListContent(
    state: ReservationListUiState,
    onBack: () -> Unit,
    onCancel: (reservationId: String) -> Unit,
) {
    // The reservation the entrant tapped Cancel on, waiting for confirmation.
    var toCancel by remember { mutableStateOf<ReservationListItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 8.dp, top = 16.dp)
        ) {
            Text("Back", color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            "My Reservations",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.reservations.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No active reservations",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.reservations, key = { it.reservation.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "CONFIRMED",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        item.campsiteName,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        formatDates(item.reservation),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Button(
                                    onClick = { toCancel = item },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                    )
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation box, so a stray tap can't cancel a trip.
    toCancel?.let { item ->
        AlertDialog(
            onDismissRequest = { toCancel = null },
            title = { Text("Cancel reservation?") },
            text = { Text("Cancel your stay at ${item.campsiteName}?") },
            confirmButton = {
                TextButton(onClick = {
                    onCancel(item.reservation.id)
                    toCancel = null
                }) { Text("Yes, cancel") }
            },
            dismissButton = {
                TextButton(onClick = { toCancel = null }) { Text("Keep it") }
            }
        )
    }
}

// Shows dates like "Jul 2 - Jul 18".
private val dayFormat = DateTimeFormatter.ofPattern("MMM d")

private fun formatDates(reservation: Reservation): String =
    "${reservation.dates.start.format(dayFormat)} - ${reservation.dates.finish.format(dayFormat)}"

@Preview(showBackground = true)
@Composable
fun ReservationsListScreenPreview() {
    CampsiteReservationAppTheme {
        ReservationListContent(
            state = ReservationListUiState(
                isLoading = false,
                reservations = listOf(
                    ReservationListItem(
                        Reservation(
                            id = "reservation-1",
                            entrantId = "entrant-priya",
                            campsiteId = "campsite-pine-ridge",
                            lotteryId = "lottery-a",
                            dates = DateRange(LocalDate.of(2027, 7, 2), LocalDate.of(2027, 7, 18)),
                        ),
                        "Pine Ridge",
                    ),
                ),
            ),
            onBack = {},
            onCancel = {},
        )
    }
}