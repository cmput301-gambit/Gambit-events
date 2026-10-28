package com.campsitereservationapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = CampPrimary,
    secondary = CampSecondary,
    tertiary = CampTertiary,
    background = CampBackground,
    surface = CampBackground,
    onPrimary = CampWhite,
    onSecondary = CampText,
    onTertiary = CampWhite,
    onBackground = CampText,
    onSurface = CampText,

)

@Composable
fun CampsiteReservationAppTheme(
    darkTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}