package com.campsitereservationapp.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.ui.components.ReservationCard
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.campsitereservationapp.model.DateRange
import com.google.apps.card.v1.OnClick
import java.time.LocalDate


@Composable
fun OwnerReservationListScreen(
    navController: NavController,
    viewModel: OwnerReservationListViewModel = viewModel(factory = OwnerReservationListViewModel.Factory),
    onReservationClick: (Reservation) -> Unit = {},
    onChatClick: (Reservation) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    OwnerReservationListContent(
        items = state.items,
        onBack = { navController.popBackStack()},
        onReservationClick= onReservationClick,
        onChatClick = onChatClick,)
}

@Composable
fun OwnerReservationListContent(
    items: List<OwnerReservationItem>,
    onBack: () -> Unit,
    onReservationClick: (Reservation) -> Unit,
    onChatClick: (Reservation) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE6DA))
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(30.dp))
        Text("Back", modifier = Modifier.clickable {onBack()})
        Text("All Reservations", fontSize = 36.sp, modifier = Modifier.padding(vertical = 24.dp))
        Text("Active", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(items, key = {it.reservation.id}) {item ->
                ReservationCard(
                    name = item.guestName,
                    partySize = null,
                    unreadCount = 0,
                    onClick = {onReservationClick(item.reservation)},
                    onChatClick = {onChatClick(item.reservation)},
                )
            }
        }
    }

}