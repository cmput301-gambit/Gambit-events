package com.campsitereservationapp.ui.discovery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

// Test daa
import com.campsitereservationapp.data.SampleData

/**
 * A temporary test screen. Feel free to replace. You can use it as an example for wiring the viewModel to the screen
 */
@Composable
fun CampsiteListScreen(
    viewModel: CampsiteListViewModel = viewModel(factory = CampsiteListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    Column(Modifier.padding(16.dp)) {
        TextField(value = state.searchQuery, onValueChange = viewModel::onSearch)
        state.campsites.forEach { Text(it.name) }
    }
}