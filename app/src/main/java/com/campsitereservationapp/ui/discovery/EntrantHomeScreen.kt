package com.campsitereservationapp.ui.discovery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.R

/**
 * Screen coinciding with M2
 * Outstanding Issues
 * -(Low priority) Currently the search button sends you to a different screen
 * while in the mockup it should all be one screen that changes when the
 * top search bar is highlighted.
 */
@Composable
fun EntrantHomeScreen(
    navController: NavController,
    viewModel: EntrantHomeScreenViewModel = viewModel(factory = EntrantHomeScreenViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()


    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Spacer(Modifier.padding(vertical = 30.dp))
            /*Search Bar NOT FINAL*/
            Button(modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.06f),
                shape = RoundedCornerShape(35f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                onClick = {
                navController.navigate("EntrantCampsiteSearch")
            }){
                Text("Search",
                    textAlign = TextAlign.Left,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(horizontal = 10.dp))
            }
            Spacer(Modifier.height(20.dp))
            /*Notifications*/
            Box(Modifier.fillMaxWidth()){
                Image(
                    painter = painterResource(R.drawable.notification_icon),
                    contentDescription = "To notifications icon",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(horizontal = 30.dp)
                        .clickable {
                            navController.navigate("Inbox")
                        }
                        .size(50.dp),
                    //contentScale = ContentScale.FillBounds
                )
            }

            Text("Welcome back,\n" + state.entrantName,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp))
            Spacer(Modifier.padding(vertical = 175.dp))
            /*Reservations*/
            Button(onClick = {
                //Navigate to reservations screen
            }
            ){
                Text("Reservations")
            }
            Row(){
                /*Button for waitlist*/
                Button(onClick = {

                }){
                    Text("Wait Lists")
                }
            }

            /* Owner Home button */
            Button(
                onClick = {
                    navController.navigate("OwnerHome")
                }
            ) {
                Text("Owner Home")
            }
        }

        /*Logo*/
        Image(
            painter = painterResource(R.drawable.primary_logo),
            contentDescription = "",
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center),
        )
    }

    Box(modifier = Modifier
        .fillMaxSize()){
        /*Settings Button*/
        Box(modifier = Modifier
            .align(Alignment.BottomStart)
            .requiredSizeIn(75.dp, 75.dp)
            .background(MaterialTheme.colorScheme.primary,
                RoundedCornerShape(30.dp)
            ),
        ){
            Image(
                painter = painterResource(R.drawable.settings_gear_icon),
                contentDescription = "To settings icon",
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable {
                        navController.navigate("UserSettings")
                    }
                    .size(40.dp),
                //contentScale = ContentScale.FillBounds
            )
        }
    }
}
