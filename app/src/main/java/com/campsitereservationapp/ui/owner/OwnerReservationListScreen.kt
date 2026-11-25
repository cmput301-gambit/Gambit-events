package com.campsitereservationapp.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.ui.components.ReservationCard
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment


/**
 * Screen displaying reservations for campsites owned by the currently logged-in owner (US 02.07.01).
 *
 * @param navController navigation controller used for back navigation
 * @param viewModel view model providing owner reservation list state
 * @param onReservationClick callback when a reservation card is clicked
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
    OwnerReservationListContent(
        state = state,
        onFilterChange = viewModel::onFilterChange,
        onBack = { navController.popBackStack() },
        onReservationClick = onReservationClick,
        onChatClick = onChatClick,
    )
}

/**
 * Layout content for owner reservation list.
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

        Text("Active", fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
                items(state.items) { reservationItem: OwnerReservationItem ->
                    ReservationCard(
                        name = "${reservationItem.guestName} - ${reservationItem.campsiteName}",
                        partySize = null,
                        unreadCount = 0,
                        onClick = { onReservationClick(reservationItem.reservation) },
                        onChatClick = { onChatClick(reservationItem.reservation) },
                    )
                }
            }
        }
    }
}
