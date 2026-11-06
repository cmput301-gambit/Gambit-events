package com.campsitereservationapp.ui.campsite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campsitereservationapp.ui.theme.CampsiteReservationAppTheme
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

/**
 * Reservation List Screen (US 01.06.01, 01.07.01)
 *
 * Purpose: Show's the reservations where they're able to cancel and confirm cancellation.
 * Back button will return to home screen.
 *
 * Design Rationale: Reliant on components from the theme that's reused so screen matches the
 * Figma.
 *
 * Outstanding Issues:
 * Reservations are local so they're not dynamic to the firebase
 * Model & UI are not separate
 *
 * TODO: replace temporary list with Firebase reservation repository and
 * calls function to remove instead of removing from the list made locally
 *
 * TODO: Separate UI screen and functionality to ViewModel so it's used
 * throughout the files.
 *
 * @param navController used by back button to return and go to home page
 * @param viewModel used by entrant active reservations and deals with cancelling
 */

@Composable
fun ReservationListScreen(
    navController: NavController
) {
    // Fake data for now. Swap for the real list once Firebase is hooked up.
    val reservations = remember {
        mutableStateListOf(
            "Old Mountain Campsite A",
            "Lakeside Camp",
            "Pine Ridge"
        )
    }
    // Function will change after ReservationRepo merge
    var toCancel by remember { mutableStateOf<String?>(null) }

    Column(
        //fixes the background colour to be consistent with the UI
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        //creates the back button so entrant can go back
        TextButton(
            onClick = { navController.popBackStack()},
            modifier = Modifier.padding(start = 8.dp, top = 16.dp)
        ) {
            Text("Back", color = MaterialTheme.colorScheme.onBackground)
        }
        //Shows the title of My Reservations
        Text(
            "My Reservations",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        //Space so it looks stylistic
        Spacer(Modifier.height(16.dp))

    //this checks if the reservations are empty and will change columns
    if (reservations.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //if there aren't any, display this text
            Text(
                "No active reservations",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else { //If not empty
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            //shows the different reservations and how they are displayed, just like Figma mockup
            items(reservations) { name ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    //another stylistic choice to align it when it displays on the UI
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //shows on what's on each card and what is being displayed on the UI
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "CONFIRMED",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                name,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Jul 2 - Jul 18",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        // Also with a cancel button being in there
                        Button(
                            onClick = { toCancel = name },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
    //This function is the confirmation box and if the person wants to cancel they'll have to confirm
    toCancel?.let { n ->
        AlertDialog(
            //Alerts and makes sure entrant wants to cancel reservation
            onDismissRequest = { toCancel = null },
            title = { Text("Cancel reservation?") },
            text = { Text("Cancel your stay at $n?") },
            confirmButton = {
                TextButton(onClick = {
                    reservations.remove(n)
                    toCancel = null
                }) { Text("Yes, cancel") }
            },
            //Gives them the option not to
            dismissButton = {
                TextButton(onClick = { toCancel = null }) { Text("Keep it") }
                }
            )
        }
    }
}

//Shows the actual preview of the app
@Preview(showBackground = true)
@Composable
fun ReservationsListScreenPreview() {
    CampsiteReservationAppTheme {
        ReservationListScreen(rememberNavController())
    }
}