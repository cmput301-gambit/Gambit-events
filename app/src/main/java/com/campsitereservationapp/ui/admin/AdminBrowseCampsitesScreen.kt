package com.campsitereservationapp.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.campsitereservationapp.R

/**
 * Screen for admin campsite browsing section matching mockup design (US 03.04.01).
 *
 * **Purpose:** provides options to view the list of all campsites or view all notifications.
 *
 * **Design rationale:** styled layout matching mockup design with dark green title,
 * custom pill buttons, and app logo header.
 *
 * **Outstanding issues:** none.
 *
 * @param navController navigation controller used for screen transitions
 */
@Composable
fun AdminBrowseCampsitesScreen(
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE6DA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Top Header Row (Back Arrow and Logo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Black
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Image(
                    painter = painterResource(R.drawable.primary_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.height(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Title
            Text(
                text = "Browse",
                fontSize = 40.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Campsites",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Button 1: View List of Campsites
            Button(
                onClick = { navController.navigate("AdminCampsiteList") },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4C6B3E),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "View List of Campsites",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button 2: View All Notifications
            Button(
                onClick = {/* Placeholder until 03.08.01 */ },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCCCCCC),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "View All Notifications",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
