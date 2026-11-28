package com.campsitereservationapp.ui.reservations

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.campsitereservationapp.ui.theme.CampPrimary
import com.campsitereservationapp.ui.theme.CampTertiary
import com.campsitereservationapp.ui.theme.CampWhite
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.ui.discovery.DateFilterMenu
import android.net.Uri
import android.util.Log

/**
 * Screen where a lottery winner sees the free dates and reserves some
 * (US 01.04.01, 01.05.01).
 *
 * **Purpose:** shows the campsite name and its free date ranges, lets the
 * user pick check-in and check-out in a date range picker, and shows a
 * confirmation once reserved: the campsite details, the booked dates, and
 * when the reservation was made. Anyone who can't reserve sees why instead
 * of the form. Before reserving, the entrant must tick "Read & Agree to Rules
 * and Policy" on the confirmation card (mockup M34, US 02.11.01).
 *
 * **Outstanding issues:** plain styling. The picker lets you select days
 * outside the free ranges, and picking one shows an error.
 *
 * @param navController navigation controller used to go back
 * @param lotteryId the lottery the user won
 * @param viewModel view model holding the reserve state
 */
@Composable
fun ReserveScreen(
    navController: NavController,
    lotteryId: String,
    viewModel: ReserveViewModel = viewModel(factory = ReserveViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(lotteryId) { viewModel.load(lotteryId) }

    val confirmation = state.confirmation
    val blockedReason = state.blockedReason

    //Sending CSV code
    val context = LocalContext.current //context for writing CSV

    //Creating launcher for making csv
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if(uri != null){
            viewModel.sendCSV(context, uri)
        }else{
            Log.d("Csv", "Failed to get uri.")
        }
    }

    LaunchedEffect(state.csvRequested) {
        if (state.csvRequested) {
            launcher.launch("reservation.csv")
            viewModel.onCsvLaunched()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Reserve your dates", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> Text("Loading...")
            confirmation != null -> ConfirmationSection(
                campsiteName = state.campsiteName,
                campsiteLocation = state.campsiteLocation,
                campsiteCapacity = state.campsiteCapacity,
                reservation = confirmation,
                onDone = { navController.popBackStack() },
            )
            blockedReason != null -> Text(blockedReason)
            else -> ReserveForm(state, viewModel,
                onReserveClicked = {
                    viewModel.reserve()
                },
                onCancelClicked = { navController.popBackStack() },
            )
        }

    }
}

/**
 * The free dates, the date picker button, and the rules confirmation card.
 *
 * @param state what to show
 * @param viewModel receives the picked dates and the Reserve tap
 * @param onReserveClicked callback when the green confirm button is tapped
 * @param onCancelClicked callback when the red cancel button is tapped
 */
@Composable
private fun ReserveForm(
    state: ReserveUiState,
    viewModel: ReserveViewModel,
    onReserveClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val checkIn = state.checkIn
    val checkOut = state.checkOut

    Text(state.campsiteName, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Free dates:")
    state.freeRanges.forEach { range -> Text("${range.start} to ${range.finish}") }
    Spacer(modifier = Modifier.height(16.dp))

    TextButton(onClick = { showPicker = true }) {
        Text(
            if (checkIn != null && checkOut != null) "$checkIn to $checkOut"
            else "Choose dates"
        )
    }
    if (showPicker) {
        DateFilterMenu(
            onConfirm = viewModel::onDatesPicked,
            onDismiss = { showPicker = false },
        )
    }

    state.error?.let { message ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.error)
    }
    Spacer(modifier = Modifier.height(16.dp))

    //CSV Toggle
    Row(){
        Text("I want a downloadable CSV of my reservation.")
        Checkbox(
            onCheckedChange = viewModel::onToggleReceiveCSV,
            checked = state.sendCSV
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    RulesConfirmationCard(
        entrantName = state.entrantName,
        campsiteCapacity = state.campsiteCapacity,
        rules = state.campsiteRules,
        rulesAccepted = state.rulesAccepted,
        canConfirm = state.canConfirm,
        onRulesAcceptedChange = viewModel::onRulesAcceptedChange,
        onCancel = onCancelClicked,
        onConfirm = onReserveClicked,
    )
}

/**
 * Card where the entrant agrees to the campsite rules and confirms the
 * reservation (mockup M34 marker 1, US 02.11.01).
 *
 * **Purpose:** shows who is reserving and for how many people, a "Read &
 * Agree to Rules and Policy" checkbox whose label opens the rules, and red
 * (cancel) and green (confirm) circle buttons. Confirm stays disabled until
 * the box is ticked.
 *
 * **Design rationale:** stateless; the agreement lives in [ReserveUiState] so
 * the gate is unit-testable in [ReserveViewModel]. Only whether the rules
 * dialog is open is kept here, because it is pure UI.
 *
 * **Outstanding issues:** the mockup's profile picture is left out for now.
 * Party size shows the campsite's capacity, since reservations don't record
 * a guest count yet.
 *
 * @param entrantName the entrant's profile name, or blank if they haven't set one
 * @param campsiteCapacity how many people the campsite holds, as text
 * @param rules the campsite's rules and policies, or blank if none were set
 * @param rulesAccepted whether the checkbox is ticked
 * @param canConfirm whether the green confirm button is enabled
 * @param onRulesAcceptedChange called when the checkbox is ticked or unticked
 * @param onCancel called when the red button is tapped
 * @param onConfirm called when the green button is tapped
 */
@Composable
private fun RulesConfirmationCard(
    entrantName: String,
    campsiteCapacity: String,
    rules: String,
    rulesAccepted: Boolean,
    canConfirm: Boolean,
    onRulesAcceptedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    var showRules by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CampTertiary)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = entrantName.ifBlank { "You" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (campsiteCapacity.isNotBlank()) {
            Text("$campsiteCapacity people", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rulesAccepted, onCheckedChange = onRulesAcceptedChange)
            Text(
                text = "Read & Agree to Rules and Policy",
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { showRules = true },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleActionButton(
                icon = Icons.Default.Close,
                contentDescription = "Cancel reservation",
                color = Color(0xFFE53935),
                enabled = true,
                onClick = onCancel,
            )
            CircleActionButton(
                icon = Icons.Default.Check,
                contentDescription = "Confirm reservation",
                color = CampPrimary,
                enabled = canConfirm,
                onClick = onConfirm,
            )
        }
    }

    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            title = { Text("Rules and Policy") },
            text = {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(rules.ifBlank { "The owner hasn't listed any rules for this campsite." })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onRulesAcceptedChange(true)
                    showRules = false
                }) { Text("I Agree") }
            },
            dismissButton = {
                TextButton(onClick = { showRules = false }) { Text("Close") }
            },
        )
    }
}

/**
 * A large coloured circle button with an icon, as on mockup M34.
 *
 * @param icon the icon drawn in the circle
 * @param contentDescription what the button does, for screen readers
 * @param color the circle's colour when enabled
 * @param enabled whether the button can be tapped
 * @param onClick called when tapped
 */
@Composable
private fun CircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = color,
            contentColor = CampWhite,
            disabledContainerColor = color.copy(alpha = 0.35f),
            disabledContentColor = CampWhite.copy(alpha = 0.7f),
        ),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(36.dp))
    }
}

/**
 * The confirmation shown once a reservation is saved.
 *
 * @param campsiteName the reserved campsite
 * @param campsiteLocation where the campsite is
 * @param campsiteCapacity how many guests the campsite holds
 * @param reservation the saved reservation, including when it was made
 * @param onDone called when the user taps Done
 */
@Composable
private fun ConfirmationSection(
    campsiteName: String,
    campsiteLocation: String,
    campsiteCapacity: String,
    reservation: Reservation,
    onDone: () -> Unit,
) {
    Text("Reservation confirmed", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(campsiteName)
    Text("Location: $campsiteLocation")
    Text("Capacity: $campsiteCapacity")
    Text("${reservation.dates.start} to ${reservation.dates.finish}")
    Text("Booked at: ${reservation.createdAt.toString().replace('T', ' ')}")
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onDone) { Text("Done") }
}
