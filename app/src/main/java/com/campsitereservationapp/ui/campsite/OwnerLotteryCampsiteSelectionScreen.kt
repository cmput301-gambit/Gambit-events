package com.campsitereservationapp.ui.campsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteError
import com.campsitereservationapp.model.LotteryError

// TODO: UPDATE COMMENTS, CREATING A LOTTERY WITH A CAMPSITE ALREADY WITH A LOTTERY WILL CREATE A NEW
//  LOTTERY ENTRY INSTEAD OF THROWING AN ERROR OR REPLACING.

/**
 * Screen for registering a lottery (US 02.04.01, US 02.04.03).
 *
 * **Purpose:** allows campsite owners to enter which campsites to set up a lottery for, including
 * registration, reservation, and winner amounts.
 *
 * **Design rationale:** collects state from [CampsiteFormViewModel], renders
 * text input fields, displays field validation errors inline, and automatically
 * navigates back once the campsite is successfully saved to the repository.
 *
 * **Outstanding issues:** image picker and interactive map coordinate selection not yet integrated.
 *
 * @param navController navigation controller used to return to the previous screen on completion
 * @param viewModel view model managing form UI state and save logic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerLotteryCampsiteSelectionScreen(
    navController: NavController,
    viewModel: LotteryFormViewModel = viewModel(factory = LotteryFormViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val campsites = remember { mutableStateListOf<Campsite>()}
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.popBackStack()
        }
        campsites.addAll(viewModel.getCampsites())

    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)

            //.verticalScroll(rememberScrollState())
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Which campsites would you like to put a lottery for?",
            style = MaterialTheme.typography.headlineSmall
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            items(campsites) { campsite ->
                CampsiteRow(campsite, viewModel)
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                viewModel.onCampsiteIdsChange()
                navController.navigate("OwnerLotteryDetails")
            }
        ) {
            Text("NEXT")
        }

    }


}

@Composable
fun CampsiteRow(campsite: Campsite, viewModel: LotteryFormViewModel) {
    val checked = remember { mutableStateOf(false)}
    Row() {
        Checkbox(
            checked = checked.value,
            onCheckedChange = {
                checked.value = it
                if (!it) {
                    viewModel.onCampsitesChangeRemove(campsite.id)
                } else {
                    viewModel.onCampsitesChangeAdd(campsite.id)
                }
            },
        )
        Text(campsite.name)
    }
}