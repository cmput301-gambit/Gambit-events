package com.campsitereservationapp.ui.reservations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.ui.discovery.DateFilterMenu

/**
 * Screen where a lottery winner sees the free dates and reserves some
 * (US 01.04.01, 01.05.01).
 *
 * **Purpose:** shows the campsite name and its free date ranges, lets the
 * user pick check-in and check-out in a date range picker, and shows a
 * confirmation once reserved: the campsite details, the booked dates, and
 * when the reservation was made. Anyone who can't reserve sees why instead
 * of the form.
 *
 * **Outstanding issues:** plain styling. The picker lets you select days
 * outside the free ranges, and picking one shows an error.
 *
 * @param navController navigation controller used to go back
 * @param lotteryId the lottery the user won
 * @param viewModel view model holding the reserve state
 */
@Composable
fun ReserveScreen(
    navController: NavController,
    lotteryId: String,
    viewModel: ReserveViewModel = viewModel(factory = ReserveViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(lotteryId) { viewModel.load(lotteryId) }

    val confirmation = state.confirmation
    val blockedReason = state.blockedReason

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Reserve your dates", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> Text("Loading...")
            confirmation != null -> ConfirmationSection(
                campsiteName = state.campsiteName,
                campsiteLocation = state.campsiteLocation,
                campsiteCapacity = state.campsiteCapacity,
                reservation = confirmation,
                onDone = { navController.popBackStack() },
            )
            blockedReason != null -> Text(blockedReason)
            else -> ReserveForm(state, viewModel)
        }
    }
}

/**
 * The free dates, the date picker button, and the Reserve button.
 *
 * @param state what to show
 * @param viewModel receives the picked dates and the Reserve tap
 */
@Composable
private fun ReserveForm(state: ReserveUiState, viewModel: ReserveViewModel) {
    var showPicker by remember { mutableStateOf(false) }
    val checkIn = state.checkIn
    val checkOut = state.checkOut

    Text(state.campsiteName, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Free dates:")
    state.freeRanges.forEach { range -> Text("${range.start} to ${range.finish}") }
    Spacer(modifier = Modifier.height(16.dp))

    TextButton(onClick = { showPicker = true }) {
        Text(
            if (checkIn != null && checkOut != null) "$checkIn to $checkOut"
            else "Choose dates"
        )
    }
    if (showPicker) {
        DateFilterMenu(
            onConfirm = viewModel::onDatesPicked,
            onDismiss = { showPicker = false },
        )
    }

    state.error?.let { message ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.error)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = viewModel::reserve) { Text("Reserve") }
}

/**
 * The confirmation shown once a reservation is saved.
 *
 * @param campsiteName the reserved campsite
 * @param campsiteLocation where the campsite is
 * @param campsiteCapacity how many guests the campsite holds
 * @param reservation the saved reservation, including when it was made
 * @param onDone called when the user taps Done
 */
@Composable
private fun ConfirmationSection(
    campsiteName: String,
    campsiteLocation: String,
    campsiteCapacity: String,
    reservation: Reservation,
    onDone: () -> Unit,
) {
    Text("Reservation confirmed", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(campsiteName)
    Text("Location: $campsiteLocation")
    Text("Capacity: $campsiteCapacity")
    Text("${reservation.dates.start} to ${reservation.dates.finish}")
    Text("Booked at: ${reservation.createdAt.toString().replace('T', ' ')}")
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onDone) { Text("Done") }
}