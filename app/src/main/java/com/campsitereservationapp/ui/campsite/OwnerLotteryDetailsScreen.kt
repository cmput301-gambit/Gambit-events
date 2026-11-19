package com.campsitereservationapp.ui.campsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteError
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.LotteryError
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun OwnerLotteryDetailsScreen(
    navController: NavController,
    viewModel: LotteryFormViewModel = viewModel(factory = LotteryFormViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val campsites = remember { mutableStateListOf<Campsite>()}
    val pickerStateRegistration = rememberDateRangePickerState()
    val pickerStateReservation = rememberDateRangePickerState()
    val registrationPeriodStart = pickerStateRegistration.selectedStartDateMillis
    val registrationPeriodEnd = pickerStateRegistration.selectedEndDateMillis
    val reservationPeriodStart = pickerStateReservation.selectedStartDateMillis
    val reservationPeriodEnd = pickerStateReservation.selectedEndDateMillis
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.popBackStack()
        }
        campsites.addAll(viewModel.getCampsites())
    }

    LaunchedEffect(registrationPeriodStart, registrationPeriodEnd) {
        if (registrationPeriodStart != null && registrationPeriodEnd != null) {

            viewModel.onRegistrationPeriodChange(DateRange(registrationPeriodStart.toLocalDate(),
                registrationPeriodEnd.toLocalDate()))
        }
    }

    LaunchedEffect(reservationPeriodStart, reservationPeriodEnd) {
        if (reservationPeriodStart != null && reservationPeriodEnd != null) {
            viewModel.onReservationPeriodChange(DateRange(reservationPeriodStart.toLocalDate(),
                reservationPeriodEnd.toLocalDate()))
        }
    }

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
            text = "Create your Lottery",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selected campsite ids
        // Registration Date Period
        val dateRangeModifier = Modifier.height(500.dp)
        Text("Registration  Date Range", style = MaterialTheme.typography.labelLarge)
        DateRangePicker(
            state = pickerStateRegistration,
            modifier = dateRangeModifier,

        )

        // Reservation Date Period
        Text("Reservation Date Range", style = MaterialTheme.typography.labelLarge)
        DateRangePicker(
            state = pickerStateReservation,
            modifier = dateRangeModifier,

        )


        // Winner Amount
        Text("Amount of Lottery Winners", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = state.winnerAmount,
            onValueChange = viewModel::onWinnerAmountChange,
            placeholder = { Text("Enter Winner Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        // Display Validation Errors
        if (state.errors.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                state.errors.forEach { error ->
                    Text(
                        text = formatLotteryError(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.saveError) {
            Text(
                text = "Failed to save campsite. Please try again.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { viewModel.save() },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isSaving) "Saving..." else "Submit")
        }
    }
}


/**
 * Converts a [CampsiteError] validation enum value into a user-friendly error message string.
 *
 * @param error the validation error encountered
 * @return human-readable description of the validation rule violation
 */
private fun formatLotteryError(error: LotteryError): String {
    return when (error) {
        LotteryError.NO_CAMPSITES -> "Campsite(s) not provided."
        LotteryError.REGISTRATION_PERIOD_AFTER_RESERVATION_PERIOD -> "Registration period cannot be after the reservation period."
        LotteryError.WINNER_AMOUNT_LESS_THAN_ONE -> "Winner amount must be greater than 0."
        LotteryError.CAPACITY_NOT_A_NUMBER -> "Winner amount must be a valid number."
        LotteryError.REGISTRATION_PERIOD_IN_PAST -> "Registration period cannot be set in the past."
        LotteryError.RESERVATION_PERIOD_IN_PAST -> "Reservation period cannot be set in the past."
        LotteryError.INVALID_WAITING_LIST -> "Waiting list must be valid."
        LotteryError.NO_OWNER_ID -> "Owner must be identified."
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate() //Currently uses UTC, not sure if this needs to be changed