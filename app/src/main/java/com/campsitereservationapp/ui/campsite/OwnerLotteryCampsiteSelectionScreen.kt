package com.campsitereservationapp.ui.campsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Campsite


/**
 * Screen for registering a lottery's campsites (US 02.04.02).
 *
 * **Purpose:** allows campsite owners to enter which campsites to set up a lottery for - allowing
 * them to proceed to a screen which would allow them to create the registration, reservation,
 * winner amounts, start stage, and waitlist capacity..
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
fun OwnerLotteryCampsiteSelectionScreen(
    navController: NavController,
    viewModel: LotteryFormViewModel = viewModel(factory = LotteryFormViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val campsites = remember { mutableStateListOf<Campsite>()}
    val campsiteLotteryDict: MutableMap<String, List<Boolean>> = remember { mutableStateMapOf() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.popBackStack()
        }
        campsites.addAll(viewModel.getCampsites())
    }

    LaunchedEffect(campsites.isNotEmpty()) {
        // Creates a map when the campsites are loaded with the campsite's id as the key it creates
        // a bool list which contains whether the campsite has a lottery (to prevent overriding)
        // and whether the campsite id is in the current lottery provided to the viewModel
        campsites.forEach { campsite ->
            campsiteLotteryDict[campsite.id] = listOf(viewModel.campsiteHasLottery(campsite.id),
                false
            )
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
            items(campsites) { campsite ->
                CampsiteRow(campsite, viewModel, campsiteLotteryDict)
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                navController.navigate("OwnerLotteryDetails")
            }
        ) {
            Text("NEXT")
        }
    }
}

@Composable
fun CampsiteRow(campsite: Campsite, viewModel: LotteryFormViewModel, campsiteLotteryDict: Map<String, List<Boolean>>) {
    val checked = remember {  mutableStateOf(campsiteLotteryDict[campsite.id]?.get(1) ?: false)}
    LaunchedEffect(campsiteLotteryDict[campsite.id]?.get(1) != null) {
        checked.value = campsiteLotteryDict[campsite.id]?.get(1) == true

    }
    val lotteryConflict = remember { mutableStateOf(false)}
    Row() {
        Checkbox(
            checked = checked.value,
            onCheckedChange = {
                checked.value = it
                if (!it) {
                    viewModel.onCampsitesChangeRemove(campsite.id)
                    lotteryConflict.value = false
                } else {
                    // Check if campsite is already in a lottery
                    if (campsiteLotteryDict[campsite.id]?.get(0) == false) {
                        viewModel.onCampsitesChangeAdd(campsite.id)
                    } else {
                        lotteryConflict.value = true
                    }
                }
            },
        )

        Text(campsite.name)
        if (lotteryConflict.value && campsiteLotteryDict[campsite.id]?.get(1) == false) {
            Text("Cannot select campsite, campsite is already linked to a lottery.", color = MaterialTheme.colorScheme.error,)
        }
    }
}