package com.campsitereservationapp.ui.campsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.CampsiteError

/**
 * Screen for registering a campsite (US 02.01.01, US 02.01.02).
 *
 * **Purpose:** allows campsite owners to enter name, location, description,
 * capacity, and tags to create a new campsite listing.
 *
 * **Design rationale:** collects state from [CampsiteFormViewModel], renders
 * text input fields, displays field validation errors inline, and automatically
 * navigates back once the campsite is successfully saved to the repository.
 *
 * **Outstanding issues:** image picker and interactive map coordinate selection not yet integrated.
 *
 * @param navController navigation controller used to return to the previous screen on completion
 * @param viewModel view model managing form UI state and save logic
 */
@Composable
fun RegisterCampsiteScreen(
    navController: NavController,
    viewModel: CampsiteFormViewModel = viewModel(factory = CampsiteFormViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.popBackStack()
        }
    }

    var tagsText by remember { mutableStateOf(state.tags.joinToString(", ")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Back",
            modifier = Modifier.clickable { navController.popBackStack() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Register your Campsite",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        Text("Name of Campsite", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            placeholder = { Text("Type Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Location
        Text("Location of Campsite", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = state.location,
            onValueChange = viewModel::onLocationChange,
            placeholder = { Text("Type Location") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text("Description of Campsite (Amenities)", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            placeholder = { Text("Type Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Maximum Capacity
        Text("Maximum Capacity", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = state.capacity,
            onValueChange = viewModel::onCapacityChange,
            placeholder = { Text("Type Capacity Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campsite Tags
        Text("Campsite Tags (Optional, comma-separated)", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = tagsText,
            onValueChange = { newTagsText ->
                tagsText = newTagsText
                val tagsList = newTagsText.split(",").map { it.trim() }
                viewModel.onTagsChange(tagsList)
            },
            placeholder = { Text("e.g. WiFi, Power, Lakefront") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display Validation Errors
        if (state.errors.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                state.errors.forEach { error ->
                    Text(
                        text = formatCampsiteError(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.saveError) {
            Text(
                text = "Failed to save campsite. Please try again.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { viewModel.save() },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isSaving) "Saving..." else "Submit")
        }
    }
}

/**
 * Converts a [CampsiteError] validation enum value into a user-friendly error message string.
 *
 * @param error the validation error encountered
 * @return human-readable description of the validation rule violation
 */
private fun formatCampsiteError(error: CampsiteError): String {
    return when (error) {
        CampsiteError.BLANK_NAME -> "Name cannot be empty."
        CampsiteError.BLANK_OWNER -> "Owner ID cannot be empty."
        CampsiteError.NON_POSITIVE_CAPACITY -> "Maximum capacity must be greater than 0."
        CampsiteError.CAPACITY_NOT_A_NUMBER -> "Maximum capacity must be a valid number."
        CampsiteError.INVALID_LATITUDE -> "Latitude coordinate is invalid."
        CampsiteError.INVALID_LONGITUDE -> "Longitude coordinate is invalid."
        CampsiteError.BLANK_TAG -> "Tags cannot contain blank items."
        CampsiteError.INVALID_AVAILABILITY -> "Availability dates are invalid."
    }
}
