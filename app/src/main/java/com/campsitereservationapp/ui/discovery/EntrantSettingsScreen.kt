package com.campsitereservationapp.ui.discovery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

/**
 * Screen coinciding with M2
 * Outstanding Issues
 * -(Low priority) Currently the search button sends you to a different screen
 * while in the mockup it should all be one screen that changes when the
 * top search bar is highlighted.
 */
@Composable
fun EntrantSettingsScreen(
    navController: NavController,
    viewModel: EntrantSettingsViewModel = viewModel(factory = EntrantSettingsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()

    Column(){
        Spacer(Modifier.padding(vertical = 30.dp))
        if(state.isUpdateUserOpen){
            Popup(

            ) {
                Box(){
                    Text("This is a popup")
                }
            }
        }
        /*Notifications*/
        Text("Name")
        Text(state.currentUser.name)
        Text("Email")
        Text(state.currentUser.email)
        Text("Phone Number")
        Text(state.currentUser.phone)
        //TODO: Add field here to change display image of user
        Text("Device ID")
        Text(state.currentUser.id) //User ID is equivalent to device ID.
        /*Toggle Update Account*/
        Button(onClick = viewModel::toggleUpdateUserOpen){
            Text("Update Account Details")
        }
        /*Toggle Receive Notifications*/
        Text("Notifications")
        Row(){
            Text("Receive Notifications")
            Button(
                onClick = viewModel::toggleReceiveNotifications
            ) {
                Text(state.currentUser.notificationsEnabled.toString()) //TODO: Make this a visual instead of text
            }
        }
        Row(){
            /*Policy and Guidelines Button*/
            Button(onClick = {
                //TODO: Add policy and guidelines screen
            }){
                Text("Policy and Guidelines")
            }

            /*Delete Account*/
            Button(onClick = {
                //TODO: Add account deletion
            }) {
                Text("Delete Account")
            }
        }
    }

}