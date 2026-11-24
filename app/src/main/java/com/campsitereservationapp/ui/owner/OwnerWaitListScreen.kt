package com.campsitereservationapp.ui.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

/**
 * Screen where a campsite owner sees who joined each of their lotteries (US 02.09.01).
 *
 * **Purpose:** lists the owner's lotteries with the campsite name, how many
 * entrants joined, and their names. Shows a loading message until the data arrives.
 *
 * **Outstanding issues:** plain styling for now.
 *
 * @param navController navigation controller used to go back
 * @param viewModel view model holding the waitlist state
 */
@Composable
fun OwnerWaitlistScreen(
    navController: NavController,
    viewModel: OwnerWaitlistViewModel = viewModel(factory = OwnerWaitlistViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Lottery Waitlists",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> Text("Loading your lotteries...")
            state.lotteries.isEmpty() -> Text("You don't have any lotteries yet.")
            else -> LazyColumn {
                items(state.lotteries, key = { it.lotteryId }) { lottery ->
                    WaitlistSection(lottery)
                }
            }
        }
    }
}

/**
 * One lottery and the names of everyone on its waitlist.
 *
 * @param lottery the lottery to show
 */
@Composable
private fun WaitlistSection(lottery: LotteryWaitlist) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(lottery.campsiteNames, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${lottery.entrantNames.size} on the waitlist",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (lottery.entrantNames.isEmpty()) {
            Text("No one has joined yet.")
        } else {
            lottery.entrantNames.forEach { name -> Text(name) }
        }
    }
    HorizontalDivider()
}