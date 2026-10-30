package com.campsitereservationapp.ui.discovery

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

// Test daa
import com.campsitereservationapp.data.SampleData
import com.campsitereservationapp.model.Campsite
import kotlin.math.roundToInt

/**
 * A temporary test screen. Feel free to replace. You can use it as an example for wiring the viewModel to the screen
 */
@Composable
fun CampsiteListScreen(
    navController: NavController,
    viewModel: CampsiteListViewModel = viewModel(factory = CampsiteListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    var displayFilters by remember { mutableStateOf(false) }

    //Checking if the row is
    val searchBarInteraction = remember { MutableInteractionSource() }
    val searchFieldInteraction = remember { MutableInteractionSource() }
    val searchBarHovered by searchBarInteraction.collectIsHoveredAsState()
    val searchFieldFocused by searchFieldInteraction.collectIsFocusedAsState()

    val showFiltersButton = searchBarHovered || searchFieldFocused || displayFilters
    Column(
        Modifier.padding(16.dp)
    ) {
        Spacer(modifier = Modifier.padding(vertical = 16.dp))
        /* Search Bar and Filters */
            //Temporary back button
            Text("Back",
                modifier = Modifier
                    .clickable(onClick =
                        {
                            navController.popBackStack()
                        }
                    )
            )
            /* Search bar */
            TextField(
                value = state.campsiteFilter.searchText,
                onValueChange = viewModel::onFilterName,
                placeholder = { Text("Search") },
                interactionSource = searchFieldInteraction
            )
            /* Filters Enable Button */
            if(showFiltersButton){
                TextButton(
                    onClick = {
                        displayFilters = !displayFilters
                    }
                ) {
                    Text("Filters")
                }
        }



        /*Filters menu*/
        if(displayFilters){
            /*Guest Capacity Field*/
            Row(){
                Text("Guest Capacity")
                Text(state.campsiteFilter.occupancyText)
                Slider(
                    value = state.campsiteFilter.occupancy.toFloat(),
                    onValueChange = { viewModel.onFilterOccupancy(it.roundToInt().toString()) },
                    valueRange = 0f..20f, //TODO: Confirm the upper bound of this field somehow.
                    steps = 19 //TODO: Step count also relies on valueRange - 1, thus same problem as above.
                )
            }
            /*Location Field*/
            Row(){
                Text("Location")
                TextField(
                    value = state.campsiteFilter.location,
                    onValueChange = viewModel::onFilterLocation
                )
            }
            /*Avaliable Dates*/
            //TODO: Implement dates sorting here after adding it to campsiteFilter

        }
        //Create all the campsite cards
        state.campsites.forEach { CampsiteSummaryCard(
            it,
            {
                navController.navigate("EntrantCampsiteDetails/" + it.id)
            }
        ) }
    }
}

@Composable
fun CampsiteSummaryCard(
    campsite: Campsite,
    onClickDetails: () -> Unit
) {
    val name: String = campsite.name
    val location: String = campsite.location
    val capacity: String = campsite.capacity.toString()
    Card(Modifier.padding(16.dp)){
        Text(name)
        //Image of campsite
        Row(){
            Column() {
                //Image of stars
                Text(location)
                Text("Capacity: $capacity")
            }
            Button(
                onClick = onClickDetails
            ) {
                Text("Details")
            }
        }
    }
}