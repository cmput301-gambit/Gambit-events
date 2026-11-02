package com.campsitereservationapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campsitereservationapp.ui.discovery.CampsiteDetailsScreen
import com.campsitereservationapp.ui.discovery.CampsiteListScreen
import com.campsitereservationapp.ui.discovery.EntrantHomeScreen

@Composable
fun CampsiteAppEntry(){
    val navController = rememberNavController()
    NavHost(navController = navController,  startDestination = "EntrantHome"){
        composable("EntrantHome") { EntrantHomeScreen(navController) } //TODO: Make this determined by the current users role
        composable("EntrantCampsiteSearch") { CampsiteListScreen(navController) }
        composable(
            route = "EntrantCampsiteDetails/{campsiteId}",
            arguments = listOf(navArgument("campsiteId"){ type = NavType.StringType})
        ) { backStackEntry ->
            val campId = backStackEntry.arguments?.getString("campsiteId") ?: return@composable
            CampsiteDetailsScreen(navController, viewedCampsiteId = campId)
        }
    }
}