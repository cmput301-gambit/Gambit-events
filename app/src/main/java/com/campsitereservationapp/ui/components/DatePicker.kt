package com.campsitereservationapp.ui.components

import android.widget.DatePicker
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CampsiteDatePicker(
    modifier: Modifier = Modifier
) {
    DatePicker(state = rememberDatePickerState())
}