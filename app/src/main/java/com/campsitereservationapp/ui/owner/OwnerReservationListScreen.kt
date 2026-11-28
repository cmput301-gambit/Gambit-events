package com.campsitereservationapp.ui.owner

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.ui.components.CircleActionButton
import com.campsitereservationapp.ui.components.ProfileAvatar
import com.campsitereservationapp.ui.components.RejectRed
import com.campsitereservationapp.ui.components.ReservationCard
import com.campsitereservationapp.ui.theme.CampPrimary
import com.campsitereservationapp.ui.theme.CampTertiary


/**
 * Screen displaying reservations for campsites owned by the currently logged-in owner
 * (US 02.07.01-US 02.07.03, 02.08.01).
 *
 * **Purpose:** lists the owner's reservations with each guest's profile picture. Tapping a
 * guest opens the reservation popup (mockup M34), where the owner can reject the reservation.
 *
 * **Outstanding issues:** none.
 *
 * @param navController navigation controller used for back navigation
 * @param viewModel view model providing owner reservation list state
 * @param onReservationClick callback when a reservation card is clicked, after the popup opens
 * @param onChatClick callback when the chat button on a reservation card is clicked
 */
@Composable
fun OwnerReservationListScreen(
    navController: NavController,
    viewModel: OwnerReservationListViewModel = viewModel(factory = OwnerReservationListViewModel.Factory),
    onReservationClick: (Reservation) -> Unit = {},
    onChatClick: (Reservation) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }

    OwnerReservationListContent(
        state = state,
        onFilterChange = viewModel::onFilterChange,
        onBack = { navController.popBackStack() },
        onReservationClick = {
            viewModel.onReservationSelected(it.id)
            onReservationClick(it)
        },
        onChatClick = onChatClick,
    )

    state.selected?.let { item ->
        ReservationPopup(
            item = item,
            isRejecting = state.isRejecting,
            onReject = viewModel::rejectSelected,
            onDismiss = viewModel::onDismissSelected,
        )
    }
}

/**
 * Layout content for owner reservation list.
 *
 * @param state what to show
 * @param onFilterChange called when the owner picks a different filter
 * @param onBack called when Back is tapped
 * @param onReservationClick called when a reservation card is tapped
 * @param onChatClick called when a card's chat button is tapped
 */
@Composable
fun OwnerReservationListContent(
    state: OwnerReservationListUiState,
    onFilterChange: (ReservationFilter) -> Unit,
    onBack: () -> Unit,
    onReservationClick: (Reservation) -> Unit,
    onChatClick: (Reservation) -> Unit,
) {
    var menuOpen by remember {mutableStateOf(false)}

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE6DA))
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(30.dp))
        Text("Back", modifier = Modifier.clickable { onBack() })
        Text("All Reservations", fontSize = 36.sp, modifier = Modifier.padding(vertical = 16.dp))

        Box {
            Row(
                Modifier.clickable{menuOpen = true},
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Change Filter")
                Text(state.filter.label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = {menuOpen = false}) {

                ReservationFilter.entries.filter { it != state.filter }.forEach { option ->
                    DropdownMenuItem(
                        text = {Text(option.label, fontWeight = FontWeight.Bold)},
                        onClick = {
                            onFilterChange(option)
                            menuOpen = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (state.isLoading) {
            Text("Loading reservations...")
        } else if (state.items.isEmpty()) {
            Text(
                text = "No reservations found for your campsites.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(state.items, key = {it.reservation.id}) { reservationItem: OwnerReservationItem ->
                    ReservationCard(
                        name = "${reservationItem.guestName} - ${reservationItem.campsiteName}",
                        partySize = null,
                        unreadCount = 0,
                        onClick = { onReservationClick(reservationItem.reservation) },
                        onChatClick = { onChatClick(reservationItem.reservation) },
                        profileImage = reservationItem.guestProfileImage,
                    )
                }
            }
        }
    }
}

/**
 * The reservation popup from mockup M34, where the owner can reject a booking (US 02.08.01).
 *
 * **Purpose:** shows the guest's profile picture, name, party size, campsite and dates. The red
 * circle (mockup marker 2) asks the owner for a reason and to confirm, then rejects the
 * reservation; the green circle closes the popup and keeps it. Rejected reservations show the
 * reason that was given.
 *
 * **Design rationale:** stateless apart from whether the confirm step is open; the rejection
 * itself runs in [OwnerReservationListViewModel.rejectSelected]. Reject only shows for active
 * reservations, since cancelled or rejected ones have nothing left to reject.
 *
 * **Outstanding issues:** party size is the campsite's capacity until reservations store a
 * guest count.
 *
 * @param item the reservation to show
 * @param isRejecting true while the rejection is being saved; disables the buttons
 * @param onReject called with the owner's written reason when they confirm the rejection
 * @param onDismiss called to close the popup without changes
 */
@Composable
private fun ReservationPopup(
    item: OwnerReservationItem,
    isRejecting: Boolean,
    onReject: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    val reservation = item.reservation

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CampTertiary)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileAvatar(imageData = item.guestProfileImage, size = 92.dp)
            Spacer(Modifier.height(16.dp))
            Text(item.guestName, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            item.partySize?.let { Text("$it people") }
            Spacer(Modifier.height(8.dp))
            Text(item.campsiteName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${reservation.dates.start} to ${reservation.dates.finish}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(24.dp))

            if (item.canReject) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CircleActionButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Reject reservation",
                        color = RejectRed,
                        enabled = !isRejecting,
                        onClick = { confirming = true },
                    )
                    CircleActionButton(
                        icon = Icons.Default.Check,
                        contentDescription = "Keep reservation",
                        color = CampPrimary,
                        enabled = !isRejecting,
                        onClick = onDismiss,
                    )
                }
            } else {
                Text(
                    if (reservation.isActive) "This stay has ended."
                    else "This reservation is ${reservation.status.name.lowercase()}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (reservation.rejectionReason.isNotBlank()) {
                    Text(
                        "Reason: ${reservation.rejectionReason}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Reject reservation?") },
            text = {
                Column {
                    Text(
                        "${item.guestName}'s booking at ${item.campsiteName} will be removed and " +
                            "they'll be notified with your reason. This can't be undone."
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { if (it.length <= Reservation.MAX_REASON_LENGTH) reason = it },
                        label = { Text("Reason") },
                        placeholder = { Text("e.g. Forest fire nearby, campsite closed") },
                        supportingText = { Text("${reason.length}/${Reservation.MAX_REASON_LENGTH}") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = reason.isNotBlank(),
                    onClick = {
                        confirming = false
                        onReject(reason)
                    },
                ) { Text("Reject", color = if (reason.isNotBlank()) RejectRed else Color.Gray) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Keep") }
            },
        )
    }
}
