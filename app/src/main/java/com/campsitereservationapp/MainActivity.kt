/*
 * Entry point of the app. Sets the theme and shows RapidRecallApp.
 *
 * Design rationale: kept minimal; all state lives in MemoryViewModel and
 * all UI lives in the ui package.
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
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