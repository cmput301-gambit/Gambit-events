package com.campsitereservationapp.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.campsitereservationapp.R

/**
 * Admin welcome and management portal screen (US 03.04.01).
 *
 * **Purpose:** provides entry points for admin moderation tasks including browsing campsites,
 * browsing users, browsing ratings, and browsing uploaded photos.
 *
 * **Design rationale:** styled layout matching mockup design with dark green title,
 * custom pill buttons, app logo header, and bottom mode indicator.
 *
 * **Outstanding issues:** Nothing checks if the user is an admin. Maybe we use a password?
 *
 * @param navController navigation controller used for screen transitions
 */
@Composable
fun AdminHomeScreen(
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

            // Top Row (Back link and Logo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2C3E2B),
                    modifier = Modifier.clickable { navController.popBackStack() }
                )

                Spacer(modifier = Modifier.weight(1f))

                Image(
                    painter = painterResource(R.drawable.primary_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.height(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Welcome Title
            Text(
                text = "Welcome,",
                fontSize = 40.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Admin",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4C6B3E)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Button 1: Browse Campsites
            Button(
                onClick = { navController.navigate("AdminBrowseCampsites") },
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
                    text = "Browse Campsites",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button 2: Browse Users
            Button(
                onClick = { navController.navigate("AdminProfileList") },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF90C2E7),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Browse Users",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button 3: Browse Ratings
            Button(
                onClick = { navController.navigate("AdminRatingList") },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE2C044),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Browse Ratings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button 4: Browse All Photos
            Button(
                onClick = { navController.navigate("AdminImageList") },
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
                    text = "Browse All Photos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Bottom Mode Text
            Text(
                text = "Viewing as: Admin",
                fontSize = 15.sp,
                color = Color.Gray,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}
