package com.campsitereservationapp.ui.components

import android.widget.DatePicker
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.tooling.preview.Preview
import com.campsitereservationapp.ui.theme.CampsiteReservationAppTheme


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CampsiteDatePicker(
    modifier: Modifier = Modifier
) {
    DatePicker(state = rememberDatePickerState())
}

@Preview(showBackground = true)
@Composable
fun CampsiteDatePickerPreview() {
    CampsiteReservationAppTheme {
        CampsiteDatePicker()
    }
}