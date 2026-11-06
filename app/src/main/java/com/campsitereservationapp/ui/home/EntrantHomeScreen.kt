package com.campsitereservationapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.R

@Composable
fun EntrantHomeScreen(
    navController: NavController,
    viewModel: EntrantHomeScreenViewModel = viewModel(factory = EntrantHomeScreenViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    EntrantHomeContent(
        state = state,
        onSearchClick = { navController.navigate("EntrantCampsiteSearch") },
        onNotificationsClick = { navController.navigate("Inbox") },
        onOwnerHomeClick = { navController.navigate("OwnerHome") },
        onSettingsClick = { navController.navigate("UserSettings") },
        onAdminHomeClick = { navController.navigate("AdminHome") },
        onReservationClick = {navController.navigate("Reservations")}
    )
}
/**
 * Screen coinciding with M2
 * Outstanding Issues
 * -(Low priority) Currently the search button sends you to a different screen
 * while in the mockup it should all be one screen that changes when the
 * top search bar is highlighted.
 */
@Composable
fun EntrantHomeContent(
    state: EntrantHomeUiState,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOwnerHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAdminHomeClick: () -> Unit,
    onReservationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    //val state by viewModel.uiState.collectAsState()


    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            //
            /*Search Bar NOT FINAL*/
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .fillMaxWidth()
                    .fillMaxHeight(0.125f)
            ){
                //Spacer(Modifier.padding(vertical = 30.dp))
                Button(modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.7f)
                    .align(Alignment.Center),
                    shape = RoundedCornerShape(45f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    onClick = onSearchClick
                ){
                    Row(horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()){
                        Image(
                            painter = painterResource(R.drawable.search_icon),
                            contentDescription = "",
                            modifier = Modifier
                                .size(120.dp)
                                .offset(x = (-40).dp)
                                //.fillMaxWidth(0.2f)
                            //.align(Alignment.Center),
                        )
                        Text("Search",
                            textAlign = TextAlign.Left,
                            fontSize = 20.sp,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .offset(x = (-70).dp)
                                //.fillMaxWidth(0.95f)
                                //.padding(horizontal = 10.dp)
                        )

                    }

                }
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
                        .clickable(onClick = onNotificationsClick)
                        .size(50.dp),
                )
            }

            Text(
                text = buildAnnotatedString {
                append("Welcome back,\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold
                    , color = MaterialTheme.colorScheme.primary)){

                        append(state.entrantName)
                    }
                },

                style = MaterialTheme.typography.bodyLarge,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .offset(x = 15.dp, y = 5.dp))

            Spacer(Modifier.padding(vertical = 125.dp))
            /*Admin Home Button*/
            Button(
                onClick = onAdminHomeClick,
                modifier = Modifier
                    .offset(x = (77.dp), y = 5.dp)
            ) {
                Text("Admin Home")
            }
            /*Reservations*/
            Button(onClick = onReservationClick,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight(0.4f)
                    .offset(x = 20.dp),
                shape = RoundedCornerShape(80f)
            ){
                Text("Reservations")
            }
            Spacer(Modifier.height(5.dp))
            Row(modifier = Modifier
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.End){
                /*Button for waitlist*/
                Button(onClick = {

                },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight(0.5f)
                        .offset(x = (-30).dp),
                    shape = RoundedCornerShape(45)){
                    Text("Wait Lists")
                }
            }

            /* Owner Home button */
            Button(
                onClick = onOwnerHomeClick,
                modifier = Modifier
                    .offset(x = (77.dp), y = 5.dp)
            ) {
                Text("Owner Home")
            }
            
        }

        /*Logo*/
        Image(
            painter = painterResource(R.drawable.primary_logo),
            contentDescription = "",
            modifier = Modifier
                .size(175.dp)
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
                RoundedCornerShape(topEnd = 100f)
            ),
        ){
            Image(
                painter = painterResource(R.drawable.settings_gear_icon),
                contentDescription = "To settings icon",
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable(onClick = onSettingsClick)
                    .size(40.dp)
                    .offset(x = (-6).dp),
            )
        }
    }
}

@Preview(showBackground = true, name = "Long name, large font", fontScale = 1.5f)
@Composable
private fun EntrantHomeLongNamePreview() {
    MaterialTheme {
        EntrantHomeContent(
            state = EntrantHomeUiState(entrantName = "Test Entrant"),
            onSearchClick = {},
            onNotificationsClick = {},
            onOwnerHomeClick = {},
            onSettingsClick = {},
            onAdminHomeClick = {},
            onReservationClick = {},
        )
    }
}