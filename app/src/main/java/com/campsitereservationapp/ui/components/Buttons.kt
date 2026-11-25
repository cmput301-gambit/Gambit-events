package com.campsitereservationapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.campsitereservationapp.ui.theme.CampHover
import com.campsitereservationapp.ui.theme.CampsiteReservationAppTheme

/*
Sources:
- Anthropic, Claude AI Sonnet 5.5 - Modifying Button and Outline for UI
- Kotlin Documentation - Structure for Button component: https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-button.html

 */

@Composable
fun PrimaryButton(
    text: String,
    onClick:() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    ) {
    Button (
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.tertiary
        )
    ) {
        Text(text)
    }
}

// @Composable - Secondary
@Composable
fun SecondaryButton(
    text: String,
    onClick:() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
) {
    Button (
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        )
    ) {
        Text(text)
    }
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text, color = MaterialTheme.colorScheme.primary)
    }
}

@Preview(showBackground = true)
@Composable
fun ButtonsPreview() {
    CampsiteReservationAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton(text = "Reserve", onClick = {})
            PrimaryButton(text = "Disabled", onClick = {}, enabled = false)
            SecondaryButton(text = "View details", onClick = {})
            OutlineButton(text = "Cancel", onClick = {})
        }
    }
}