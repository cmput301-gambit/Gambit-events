package com.campsitereservationapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campsitereservationapp.model.Reservation

/**
 * Reservation Card List
 *
 * A component that can be used by Owner/Entrant although, Entrant will require editing
 * All components are followed by the design guide provided in Figma.
 */

@Composable
fun ReservationCard(
    name: String,
    partySize: Int?,
    unreadCount: Int,
    onClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFC8C8C8))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Person,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .padding(10.dp)

        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (partySize != null) Text("$partySize person")
        }
        BadgedBox(
            badge = {
                if (unreadCount > 0) Badge {
                    Text("$unreadCount")
                }
            }
        ) {
            IconButton(
                onClick = onChatClick,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFFA0C4E8),CircleShape)
            ) {
                Icon(Icons.Filled.Email, contentDescription = "Chat")
            }
        }

    }

}