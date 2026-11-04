package com.campsitereservationapp.ui.discovery

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
fun EntrantHomeScreen(
    navController: NavController,
    viewModel: EntrantHomeScreenViewModel = viewModel(factory = EntrantHomeScreenViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()

    Column(){
        Spacer(Modifier.padding(vertical = 30.dp))
        /*Search Bar NOT FINAL*/
        Button(onClick = {
            navController.navigate("EntrantCampsiteSearch")
        }){
            Text("Search")
        }
        /*Notifications*/
        Button(onClick = {
            navController.navigate("Inbox")
        }) {
            Text("Inbox")
        }
        Text("Welcome back,\n" + state.entrantName)
        /*Reservations*/
        Button(onClick = {
            navController.navigate(route = "Reservations")
            }
        ){
            Text("Reservations")
        }
        Row(){
            /*Button for QR code reader*/
            Button(onClick = {

            }){
                Text("Wait Lists")
            }
        }
        /*Settings Button*/
        Button(
            onClick = {
                navController.navigate("UserSettings")
            }
        ) {
            Text("Settings")
        }
        /* Owner Home button (temp until we add the actual login process, testing purposes only) */
        Button(
            onClick = {
                navController.navigate("OwnerHome")
            }
        ) {
            Text("Owner Home")
        }
        /* Admin Home button (temp until we add the actual login process, testing purposes only) */
        Button(
            onClick = {
                navController.navigate("AdminHome")
            }
        ) {
            Text("Admin Home")
        }
    }

}
