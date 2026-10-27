/*
 * Entry point of the app. Sets the theme and shows CampsiteListScreen.
 *
 * Design rationale:
 *
 * Outstanding issues: none known.
 *
 * AI assistance:
 */
package com.campsitereservationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.campsitereservationapp.ui.discovery.CampsiteListScreen
import com.campsitereservationapp.ui.theme.CampsiteReservationAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampsiteReservationAppTheme {
                CampsiteListScreen()
            }
        }
    }
}