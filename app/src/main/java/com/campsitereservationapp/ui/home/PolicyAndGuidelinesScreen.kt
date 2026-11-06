package com.campsitereservationapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
/**
 * Shows the app's policy and guidelines.
 *
 * **Purpose:** lets entrants read the app's terms and guidelines from the settings screen.
 *
 * **Design rationale:** static text only, so it has no ViewModel.
 *
 * **Outstanding issues:** the policy text is a placeholder.
 *
 * @param navController used to go back to the previous screen
 */
@Composable
fun PolicyAndGuidelinesScreen(
    navController: NavController,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ){
        Spacer(modifier = Modifier.padding(vertical = 36.dp))
        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )
        Spacer(Modifier.height(50.dp))
        Text("The policy and guidelines would be located here.")
    }
}