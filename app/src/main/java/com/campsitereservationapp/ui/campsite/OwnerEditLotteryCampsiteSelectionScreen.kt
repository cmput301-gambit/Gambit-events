package com.campsitereservationapp.ui.campsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.LotteryError
import java.time.LocalDate
import java.time.temporal.ChronoUnit


/**
 * Screen for editing a lottery's campsites selection (US 02.04.02).
 *
 * **Purpose:** allows campsite owners to see which campsites are currently selected for a lottery
 * and allows them to add or remove campsites (minimum of 1 campsite, campsites cannot have more than
 * one lottery but one lottery may have more than one campsite.)
 *
 * **Design rationale:** collects state from [LotteryFormViewModel], renders
 * text input fields, displays field validation errors inline, and automatically
 * navigates back once the campsite is successfully saved to the repository.
 *
 * **Outstanding issues:**
 *
 * @param navController navigation controller used to return to the previous screen on completion
 * @param viewModel view model managing form UI state and save logic
 */
@Composable
fun OwnerEditLotteryCampsiteSelectionScreen(
    navController: NavController,
    lotteryId: String,
    viewModel: LotteryFormViewModel = viewModel(factory = LotteryFormViewModel.provideFactory(lotteryId))
) {
    val state by viewModel.uiState.collectAsState()
    val campsites = remember { mutableStateListOf<Campsite>()}
    val colourButtonContainer = MaterialTheme.colorScheme.tertiary
    val colourButtonContent = MaterialTheme.colorScheme.onTertiary
    val colourButtonContainerEnabled = MaterialTheme.colorScheme.primary
    val colourButtonContentEnabled = MaterialTheme.colorScheme.onPrimary

    val campsiteLotteryDict: MutableMap<String, List<Boolean>> = remember { mutableStateMapOf() }
    LaunchedEffect(state.isSaving) {
        if (state.isSaving) {
            navController.navigate("EditLottery/${lotteryId}")
        }
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            campsites.addAll(viewModel.getCampsites())
            campsites.forEach { campsite ->
                //viewModel.startingCampsiteIds.add(campsite.id)
                // Creates a map when the campsites are loaded with the campsite's id as the key it creates
                // a bool list which contains whether the campsite has a lottery (to prevent overriding)
                // and whether the campsite id is in the current lottery provided to the viewModel.
                campsiteLotteryDict[campsite.id] = listOf(
                    viewModel.campsiteHasLottery(campsite.id),
                    campsite.id in viewModel.returnCampsiteIds()
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Which campsites would you like to create a lottery for?",
            style = MaterialTheme.typography.headlineSmall
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (!campsites.isEmpty()) {
                items(campsites) { campsite ->
                    CampsiteRow(campsite, viewModel, campsiteLotteryDict)
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                if (campsites.isNotEmpty()) {
                    viewModel.save()
                    // overrides the registration period and reservation period when they error in saving.
                    if (viewModel.getErrors().contains(LotteryError.REGISTRATION_PERIOD_IN_PAST)) {
                        val registrationStart: LocalDate = state.registrationPeriod.start
                        viewModel.onRegistrationPeriodChange(DateRange(registrationStart, LocalDate.now()))

                        val reservationStart: LocalDate = state.reservationPeriod.start
                        if (reservationStart.isBefore(LocalDate.now())) {
                            val reservationPeriod = ChronoUnit.DAYS.between(reservationStart, state.reservationPeriod.start)
                            viewModel.onReservationPeriodChange(
                                DateRange(LocalDate.now().plusDays(1),
                                    LocalDate.now().plusDays(reservationPeriod+1)))
                        }
                        viewModel.save()
                    }
                }
            },

            containerColor = if (campsites.isNotEmpty()) colourButtonContainerEnabled else colourButtonContainer,
            contentColor = if (campsites.isNotEmpty()) colourButtonContentEnabled else colourButtonContent,
        ) {
            Text("NEXT")
        }
    }
}