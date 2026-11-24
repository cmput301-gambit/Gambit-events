package com.campsitereservationapp.ui.campsite

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Campsite


@Composable
fun OwnerEditLotteryCampsiteSelectionScreen(
    navController: NavController,
    lotteryId: String,
    viewModel: LotteryFormViewModel = viewModel(factory = LotteryFormViewModel.provideFactory(lotteryId))
) {
    val state by viewModel.uiState.collectAsState()
    val campsites = remember { mutableStateListOf<Campsite>()}

    val campsiteLotteryDict: MutableMap<String, List<Boolean>> = remember { mutableStateMapOf() }
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.popBackStack()
        }
        campsites.addAll(viewModel.getCampsites())
    }
//    LaunchedEffect(campsites.isEmpty()) {
//        temp.addAll(viewModel.getCampsites())
//    }
//    campsites.addAll(temp)

    LaunchedEffect(campsites.isNotEmpty()) {
        Log.d("CampRow", "a")
        Log.d("hasLotteryMap", "Running forEach")
        Log.d("hasLotteryMap", campsites.toString())
        campsites.forEach { campsite ->
            // For each campsite it is going to put into a map [hasLottery, is in currentLottery]
            campsiteLotteryDict[campsite.id] = listOf(viewModel.campsiteHasLottery(campsite.id), campsite.id in state.campsiteIds)
            Log.d("bool", "${campsite.id}, ${campsites}, ${campsite.id in state.campsiteIds}")
            Log.d("hasLotteryMap", "Campsite Id: ${campsite.id}, hasLottery: ${campsiteLotteryDict[campsite.id]}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)

        //.verticalScroll(rememberScrollState())
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Which campsites would you like to create a lottery for?",
            style = MaterialTheme.typography.headlineSmall
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (!campsites.isEmpty()) {
                items(campsites) { campsite ->
                    //Log.d("campy", "$campsiteLotteryDict")
                    CampsiteRow(campsite, viewModel, campsiteLotteryDict)
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                viewModel.onCampsiteIdsChange()
                navController.navigate("OwnerLotteryDetails")
                //navController.navigate("OwnerLotteryDetails/$lotteryId")
            }
        ) {
            Text("NEXT")
        }
    }
}