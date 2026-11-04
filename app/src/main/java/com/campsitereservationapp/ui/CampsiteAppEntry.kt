package com.campsitereservationapp.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campsitereservationapp.ui.admin.AdminBrowseCampsitesScreen
import com.campsitereservationapp.ui.admin.AdminCampsiteListScreen
import com.campsitereservationapp.ui.admin.AdminHomeScreen
import com.campsitereservationapp.ui.campsite.OwnerHomeScreen
import com.campsitereservationapp.ui.campsite.RegisterCampsiteScreen
import com.campsitereservationapp.ui.discovery.CampsiteDetailsScreen
import com.campsitereservationapp.ui.discovery.CampsiteListScreen
import com.campsitereservationapp.ui.discovery.EntrantHomeScreen
import com.campsitereservationapp.ui.discovery.EntrantSettingsScreen
import com.campsitereservationapp.ui.discovery.InboxScreen
import com.campsitereservationapp.ui.campsite.ReservationListScreen
/**
 * Top-level Jetpack Compose navigation entry point for the Campsite Reservation Application.
 *
 * **Purpose:** manages the navigation host graph, mapping route identifiers to composable
 * screens across entrant, campsite owner, admin, and settings flows.
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
        composable("Reservations") { ReservationListScreen(navController) }
        composable("AdminHome") { AdminHomeScreen(navController) }
        composable("AdminBrowseCampsites") { AdminBrowseCampsitesScreen(navController) }
        composable("AdminCampsiteList") { AdminCampsiteListScreen(navController) }
        composable("EntrantCampsiteSearch") { CampsiteListScreen(navController) }
        composable(
            route = "EntrantCampsiteDetails/{campsiteId}",
            arguments = listOf(navArgument("campsiteId"){ type = NavType.StringType})
        ) { backStackEntry ->
            Log.d("CampsiteAppEntry", "Requesting Details screen with id='${backStackEntry.arguments?.getString("campsiteId")}'")
            val campId = backStackEntry.arguments?.getString("campsiteId") ?: return@composable
            CampsiteDetailsScreen(navController, viewedCampsiteId = campId)
        }
        composable(
            route = "AdminCampsiteDetails/{campsiteId}",
            arguments = listOf(navArgument("campsiteId"){ type = NavType.StringType})
        ) { backStackEntry ->
            val campId = backStackEntry.arguments?.getString("campsiteId") ?: return@composable
            CampsiteDetailsScreen(navController, viewedCampsiteId = campId, isAdmin = true)
        }
        composable("UserSettings") { EntrantSettingsScreen(navController) }
        composable("Inbox") { InboxScreen(navController) }
    }
}
