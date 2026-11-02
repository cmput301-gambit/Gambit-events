package com.campsitereservationapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campsitereservationapp.ui.campsite.OwnerHomeScreen
import com.campsitereservationapp.ui.campsite.RegisterCampsiteScreen
import com.campsitereservationapp.ui.discovery.CampsiteDetailsScreen
import com.campsitereservationapp.ui.discovery.CampsiteListScreen
import com.campsitereservationapp.ui.discovery.EntrantHomeScreen
import com.campsitereservationapp.ui.discovery.EntrantSettingsScreen

/**
 * Top-level Jetpack Compose navigation entry point for the Campsite Reservation Application.
 *
 * **Purpose:** manages the navigation host graph, mapping route identifiers to composable
 * screens across entrant, campsite owner, and settings flows.
 *
 * **Design rationale:** uses Jetpack Navigation Compose with a single [rememberNavController]
 * instance to handle route switching and backstack navigation cleanly.
 *
 * **Outstanding issues:** deep-linking and bottom navigation bar integration pending.
 */
@Composable
fun CampsiteAppEntry(){
    val navController = rememberNavController()
    NavHost(navController = navController,  startDestination = "EntrantHome"){
        composable("EntrantHome") { EntrantHomeScreen(navController) }
        composable("OwnerHome") { OwnerHomeScreen(navController) }
        composable("RegisterCampsite") { RegisterCampsiteScreen(navController) }
        composable("EntrantCampsiteSearch") { CampsiteListScreen(navController) }
        composable(
            route = "EntrantCampsiteDetails/{campsiteId}",
            arguments = listOf(navArgument("campsiteId"){ type = NavType.StringType})
        ) { backStackEntry ->
            val campId = backStackEntry.arguments?.getString("campsiteId") ?: return@composable
            CampsiteDetailsScreen(navController, viewedCampsiteId = campId)
        }
        composable("UserSettings") { EntrantSettingsScreen(navController) }
    }
}
