package com.campsitereservationapp.ui.discovery

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
    var tempNewName by remember { mutableStateOf(state.currentUser.name) }
    //val tempNewPhone by remember { mutableStateOf("") }
    //val tempNewEmail by remember { mutableStateOf("") }
    /*Update Settings popup*/
    Column(Modifier.fillMaxWidth()){
        Spacer(Modifier.padding(vertical = 30.dp))
        //Temporary back button
        Text("Back",
            modifier = Modifier
                .clickable(onClick =
                    {
                        navController.popBackStack()
                    }
                )
        )
        if(state.isUpdateUserOpen){
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, 170),
                onDismissRequest = viewModel::toggleUpdateUserOpen,
                properties = PopupProperties(focusable = true),
            ) {
                Box(
                    Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .padding(12.dp)
                        .fillMaxWidth(0.75f)
                        .fillMaxHeight(0.75f)
                ){
                    Column(Modifier.fillMaxWidth()){
                        TextField(
                            value = tempNewName,
                            onValueChange = {tempNewName=it},//viewModel::onFilterName,
                            placeholder = { Text("New Name") },
                            //interactionSource = searchFieldInteraction
                        )
                        Button(onClick = {
                            viewModel.updateUserDetails(
                                tempNewName,
                                state.currentUser.phone,
                                state.currentUser.email,
                                state.currentUser.profileImageId
                            )
                            viewModel.toggleUpdateUserOpen()
                        }
                        ) {
                            Text("Update Profile")
                        }
                    }

                    //Text("This is a popup")
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

            Button(onClick = {
                if(!state.isUpdateUserOpen)
                    viewModel.toggleUpdateUserOpen()
                }
            ){
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