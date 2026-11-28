package com.campsitereservationapp.ui.campsite

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.LotteryError
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset


/**
 * Screen for editing an existing lottery's details (US 02.04.07, US 02.04.03).
 *
 * **Purpose:** allows campsite owners to edit details of an existing lottery.
 *
 * **Design rationale:** reuses [LotteryFormViewModel] via [LotteryFormViewModel.provideFactory]
 * in editing mode, pre-populating existing lottery values and saving updates.
 *
 * **Outstanding issues:**
 *
 * @param navController navigation controller used to return on completion
 * @param lotteryId ID of the lottery being edited
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

    val waitlistCapacityInitialText = remember {mutableStateOf(state.waitlistCapacity)}

    val pickerStateRegistration = rememberDateRangePickerState()
    val pickerStateReservation = rememberDateRangePickerState()
    var registrationPeriodStart = pickerStateRegistration.selectedStartDateMillis
    var registrationPeriodEnd = pickerStateRegistration.selectedEndDateMillis
    var reservationPeriodStart = pickerStateReservation.selectedStartDateMillis
    var reservationPeriodEnd = pickerStateReservation.selectedEndDateMillis

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.popBackStack()
            navController.popBackStack()
        }
        // Hides default "no waitlist cap" option.
        if (waitlistCapacityInitialText.value == "-1") {
            waitlistCapacityInitialText.value = ""
        }
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            pickerStateRegistration.setSelection(viewModel.getRegistrationRange().start, viewModel.getRegistrationRange().finish)
            pickerStateReservation.setSelection(viewModel.getReservationRange().start, viewModel.getReservationRange().finish)
            registrationPeriodStart = pickerStateRegistration.selectedStartDateMillis
            registrationPeriodEnd = pickerStateRegistration.selectedEndDateMillis
            reservationPeriodStart = pickerStateReservation.selectedStartDateMillis
            reservationPeriodEnd = pickerStateReservation.selectedEndDateMillis
        }
    }
    LaunchedEffect( registrationPeriodEnd) {
        if (registrationPeriodStart != null && registrationPeriodEnd != null) {
            viewModel.onRegistrationPeriodChange(DateRange(
                registrationPeriodStart!!.toLocalDate(),
                registrationPeriodEnd!!.toLocalDate()))
        }
    }

    LaunchedEffect( reservationPeriodEnd) {
        if (reservationPeriodStart != null && reservationPeriodEnd != null) {
            viewModel.onReservationPeriodChange(DateRange(
                reservationPeriodStart!!.toLocalDate(),
                reservationPeriodEnd!!.toLocalDate()))
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
            modifier = Modifier.clickable { navController.popBackStack()
                navController.popBackStack() }
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
        } else {
            // Registration Date Period
            // Same size as the create screen (OwnerLotteryDetailsScreen); narrower squishes the picker.
            val dateRangeModifier = Modifier
                .height(400.dp)
                .fillMaxWidth(0.8f)
                .align(Alignment.CenterHorizontally)
            Text("Registration  Date Range", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
            DateRangePicker(
                state = pickerStateRegistration,
                modifier = dateRangeModifier,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Reservation Date Period
            Text("Reservation Date Range", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
            DateRangePicker(
                state = pickerStateReservation,
                modifier = dateRangeModifier,
            )


            Row ()
            {
                // Winner Amount
                Column(modifier = Modifier.fillMaxWidth(0.5f))
                {
                    Text("Amount of Lottery Winners", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = state.winnerAmount,
                        onValueChange = viewModel::onWinnerAmountChange,
                        placeholder = { Text("Enter Winner Amount") },
                        //modifier = Modifier.fillMaxWidth(0.5f)
                    )
                }
                // Waitlist Capacity
                Column (modifier = Modifier.fillMaxWidth()) {
                    Text("Capacity of waitlist (optional)", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = viewModel.getWaitinglistCapacity(),
                        onValueChange = viewModel::onWaitlistCapacityChange,
                        placeholder = { Text("Enter Waitlist Maximum") },
                        //modifier = Modifier.fillMaxWidth(0.5f)
                    )
                }
            }

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
                onClick = {
                    viewModel.save() },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "Saving..." else "Submit")
            }
        }
    }
}



    /**
 * Converts a [LotteryError] validation enum value into a user-friendly error message string.
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
        LotteryError.INVALID_WAITING_LIST -> "Waitlist must be a valid number."
        LotteryError.NO_OWNER_ID -> "Owner must be identified."
    }
}
private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate() //Currently uses UTC, not sure if this needs to be change
