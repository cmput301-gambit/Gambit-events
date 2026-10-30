package com.campsitereservationapp.ui.discovery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campsitereservationapp.model.Campsite

/**
 * Used to represent the finite states of the main button at the bottom of the screen.
 * JOIN will attempt to join the lottery for the currently viewed campsite
 * LEAVE will attempt to remove the current user from the viewed campsite
 * UNAVAILABLE is the default case if something goes wrong (ex. the lottery closes while viewing the screen.)
 */
enum class CampsiteDetailsMainButtonState{
    JOIN,
    LEAVE,
    UNAVAILABLE
}
/**
 * Screen used to fulfill US 01.01.05, 01.01.06, 01.02.01, 01.02.02, and 01.13.01
 */
@Composable
fun CampsiteDetailsScreen(
    viewedCampsiteId : String,
    viewModel: CampsiteDetailsViewModel = viewModel(factory = CampsiteDetailsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    //Try to update the page if the campsite is invalid
    if(!state.viewedCampsite.isValid()){
        viewModel.updateViewedCampSiteId(viewedCampsiteId)
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
