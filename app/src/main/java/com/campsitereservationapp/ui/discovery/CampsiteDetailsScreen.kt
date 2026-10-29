package com.campsitereservationapp.ui.discovery

import androidx.compose.foundation.Image
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

// Test daa
import com.campsitereservationapp.data.SampleData
import com.campsitereservationapp.model.Campsite
import kotlin.math.roundToInt

enum class CampsiteDetailsMainButtonState{
    JOIN,
    LEAVE,
    UNAVAILABLE
}
/**
 * A temporary test screen. Feel free to replace. You can use it as an example for wiring the viewModel to the screen
 */
@Composable
fun CampsiteDetailsScreen(
    viewedCampsite : Campsite,
    viewModel: CampsiteDetailsViewModel = viewModel(factory = CampsiteDetailsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    //Try to update the page if the campsite is invalid
    if(!state.viewedCampsite.isValid()){
        viewModel.updateViewedCampSite(viewedCampsite)
    }


    Column(
        Modifier.padding(16.dp)
    ) {
        Spacer(modifier = Modifier.padding(vertical = 16.dp))
        /* Search Bar and Filters */
        //Leave Button
        /* Search bar */
        Text(
            state.viewedCampsite.name
        )
        //Campsite display image
        //TODO: Figure out how to display state.viewedCampsite.image here.


        Text("Capacity: " + state.viewedCampsite.capacity)//Campsite Capacity
        //Rating
        //TODO: Need to implement this visual after the rating class exists
        //Rate button
        //TODO: Popup card and button to rate, after rating class is added
        //CampsiteDescription
        Text(state.viewedCampsite.description)
        //joinLeaveButton
        Button(
            onClick = viewModel::updateButtonState
        ){
            val txt = when {
                state.mainButtonState == CampsiteDetailsMainButtonState.JOIN -> "Join Waitlist"
                state.mainButtonState == CampsiteDetailsMainButtonState.LEAVE -> "Leave Waitlist"
                state.mainButtonState == CampsiteDetailsMainButtonState.UNAVAILABLE -> "Unavailable"
                else -> "Unavailable"
            }
            Text(txt) //Join or Leave Waitlist
        }


    }
}
