package com.campsitereservationapp.ui.campsite

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.material3.setSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteError
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.LotteryError
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// TODO: UPDATE COMMENTS

/**
 * Screen for editing an existing campsite (US 02.01.03 / #20).
 *
 * **Purpose:** allows campsite owners to edit details of an existing campsite.
 *
 * **Design rationale:** reuses [CampsiteFormViewModel] via [CampsiteFormViewModel.provideFactory]
 * in editing mode, pre-populating existing campsite values and saving updates.
 *
 * **Outstanding issues:** image editing is a placeholder as image uploading has not been added yet.
 * Major styling is required.
 *
 * @param navController navigation controller used to return on completion
 * @param campsiteId ID of the campsite being edited
 * @param viewModel view model initialized in editing mode
 */
@SuppressLint("UnrememberedMutableState")
@Composable
fun EditLotteryScreen(
    navController: NavController,
    lotteryId: String,
    viewModel: LotteryFormViewModel = viewModel(factory = LotteryFormViewModel.provideFactory(lotteryId))
) {
    val state by viewModel.uiState.collectAsState()
    val pickerStateRegistration = rememberDateRangePickerState(initialSelectedStartDate = viewModel.getRegistrationRange().start,
        initialSelectedEndDate = viewModel.getRegistrationRange().finish)
    val pickerStateReservation = rememberDateRangePickerState(initialSelectedStartDate = viewModel.getReservationRange().start,
        initialSelectedEndDate = viewModel.getReservationRange().finish)
    val registrationPeriodStart = pickerStateRegistration.selectedStartDateMillis
    val registrationPeriodEnd = pickerStateRegistration.selectedEndDateMillis
    val reservationPeriodStart = pickerStateReservation.selectedStartDateMillis
    val reservationPeriodEnd = pickerStateReservation.selectedEndDateMillis

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.popBackStack()
        }
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
            text = "Edit your Lottery",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))
        if (state.isLoading) {
            Text("Loading campsite details...")
        } else if (state.loadError) {
            Text("Failed to load lottery details.", color = MaterialTheme.colorScheme.error)
            Log.d("EditLotteryScreen", "Passed LotteryId: $lotteryId")
        } else {

            // Registration Date Period
            val dateRangeModifier = Modifier
                .height(400.dp)
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterHorizontally)
            Text("Registration  Date Range", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            DateRangePicker(
                state = pickerStateRegistration,
                modifier = dateRangeModifier,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Reservation Date Period
            Text("Reservation Date Range", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
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

            MinimalDropdownMenu(viewModel, mutableStateOf(state.lotteryStage))

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
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate() //Currently uses UTC, not sure if this needs to be change
