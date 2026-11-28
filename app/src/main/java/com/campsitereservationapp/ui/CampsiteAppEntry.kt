package com.campsitereservationapp.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campsitereservationapp.ui.admin.AdminBrowseCampsitesScreen
import com.campsitereservationapp.ui.admin.AdminCampsiteListScreen
import com.campsitereservationapp.ui.admin.AdminHomeScreen
import com.campsitereservationapp.ui.admin.AdminImageListScreen
import com.campsitereservationapp.ui.admin.AdminProfileListScreen
import com.campsitereservationapp.ui.admin.AdminRatingListScreen
import com.campsitereservationapp.ui.campsite.BookedDatesScreen
import com.campsitereservationapp.ui.campsite.EditCampsiteScreen
import com.campsitereservationapp.ui.campsite.EditLotteryScreen
import com.campsitereservationapp.ui.campsite.LotteryFormViewModel
import com.campsitereservationapp.ui.campsite.OwnerEditLotteryCampsiteSelectionScreen
import com.campsitereservationapp.ui.campsite.OwnerLotteryCampsiteSelectionScreen
import com.campsitereservationapp.ui.campsite.OwnerLotteryDetailsScreen
import com.campsitereservationapp.ui.owner.OwnerCampsiteDetailsScreen
import com.campsitereservationapp.ui.owner.OwnerCampsiteListScreen
import com.campsitereservationapp.ui.owner.OwnerHomeScreen
import com.campsitereservationapp.ui.owner.OwnerWaitlistScreen
import com.campsitereservationapp.ui.owner.NotifyWaitlistScreen
import com.campsitereservationapp.ui.owner.SentNotificationsScreen
import com.campsitereservationapp.ui.campsite.RegisterCampsiteScreen
import com.campsitereservationapp.ui.campsite.CampsiteDetailsScreen
import com.campsitereservationapp.ui.discovery.CampsiteListScreen
import com.campsitereservationapp.ui.home.EntrantHomeScreen
import com.campsitereservationapp.ui.profile.EntrantSettingsScreen
import com.campsitereservationapp.ui.notifications.InboxScreen
import com.campsitereservationapp.ui.reservations.ReservationListScreen
import com.campsitereservationapp.ui.reservations.ReserveScreen
import com.campsitereservationapp.ui.home.PolicyAndGuidelinesScreen
import com.campsitereservationapp.ui.messages.ConversationScreen
import com.campsitereservationapp.ui.messages.OwnerGuestsScreen
import com.campsitereservationapp.ui.owner.OwnerReservationListScreen

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
        composable("OwnerLotteryCampsiteSelectionScreen") { OwnerLotteryCampsiteSelectionScreen(navController) }
        composable("OwnerLotteryDetails") { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("OwnerLotteryCampsiteSelectionScreen")
            }
            val viewModel: LotteryFormViewModel = viewModel(viewModelStoreOwner = parentEntry)
            OwnerLotteryDetailsScreen(navController, viewModel=viewModel)
        }
        composable(
            route="EditLottery/{lotteryId}",
            arguments = listOf(navArgument("lotteryId") { type = NavType.StringType})
        ) { backStackEntry ->
            val lotteryId = backStackEntry.arguments?.getString("lotteryId") ?: return@composable
            EditLotteryScreen(navController, lotteryId = lotteryId)
        }
        composable(
            route="OwnerEditLotteryCampsiteSelectionScreen/{lotteryId}",
            arguments = listOf(navArgument("lotteryId") { type = NavType.StringType})
        ) { backStackEntry ->
            val lotteryId = backStackEntry.arguments?.getString("lotteryId") ?: return@composable
            OwnerEditLotteryCampsiteSelectionScreen(navController, lotteryId = lotteryId)
        }
        composable("Reservations") { ReservationListScreen(navController) }
        composable("OwnerCampsiteList") { OwnerCampsiteListScreen(navController) }
        composable(
            route = "OwnerCampsiteDetails/{campsiteId}",
            arguments = listOf(navArgument("campsiteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val campId = backStackEntry.arguments?.getString("campsiteId") ?: return@composable
            OwnerCampsiteDetailsScreen(navController, campsiteId = campId)
        }
        composable(
            route = "BookedDates/{campsiteId}",
            arguments = listOf(navArgument("campsiteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val campId = backStackEntry.arguments?.getString("campsiteId") ?: return@composable
            BookedDatesScreen(navController, campsiteId = campId)
        }
        composable(
            route = "EditCampsite/{campsiteId}",
            arguments = listOf(navArgument("campsiteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val campId = backStackEntry.arguments?.getString("campsiteId") ?: return@composable
            EditCampsiteScreen(navController, campsiteId = campId)
        }
        composable("AdminHome") { AdminHomeScreen(navController) }
        composable("AdminBrowseCampsites") { AdminBrowseCampsitesScreen(navController) }
        composable("AdminCampsiteList") { AdminCampsiteListScreen(navController) }
        composable("AdminProfileList") { AdminProfileListScreen(navController) }
        composable("AdminRatingList") { AdminRatingListScreen(navController) }
        composable("AdminImageList") { AdminImageListScreen(navController) }
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
        composable("OwnerWaitlists") { OwnerWaitlistScreen(navController) }
        composable(
            route = "NotifyWaitlist/{lotteryId}",
            arguments = listOf(navArgument("lotteryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val lotteryId = backStackEntry.arguments?.getString("lotteryId") ?: return@composable
            NotifyWaitlistScreen(navController, lotteryId = lotteryId)
        }
        composable("SentNotifications") { SentNotificationsScreen(navController) }
        composable("Inbox") { InboxScreen(navController) }
        composable(
            route = "Reserve/{lotteryId}",
            arguments = listOf(navArgument("lotteryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val lotteryId = backStackEntry.arguments?.getString("lotteryId") ?: return@composable
            ReserveScreen(navController, lotteryId = lotteryId)
        }
        composable("PolicyAndGuidelines") { PolicyAndGuidelinesScreen(navController) }
        composable(
            route = "OwnerGuests/{campsiteId}",
            arguments = listOf(navArgument("campsiteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val campId = backStackEntry.arguments?.getString("campsiteId") ?: return@composable
            OwnerGuestsScreen(navController, campsiteId = campId)
        }
        composable(
            route = "Conversation/{reservationId}",
            arguments = listOf(navArgument("reservationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reservationId = backStackEntry.arguments?.getString("reservationId") ?: return@composable
            ConversationScreen(navController, reservationId = reservationId)
        }
        composable("OwnerReservations") { OwnerReservationListScreen(navController) }
    }
}
