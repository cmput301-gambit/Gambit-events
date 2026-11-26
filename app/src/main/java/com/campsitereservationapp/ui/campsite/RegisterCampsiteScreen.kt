package com.campsitereservationapp.ui.campsite

import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.model.CampsiteError

/**
 * Decodes a local image [Uri] into a Compose [ImageBitmap] safely across Android API levels,
 * downscaling larger images to 512px max dimension to prevent main-thread OOM issues.
 *
 * @param uri local media URI selected by the photo picker
 * @return decoded [ImageBitmap] or null if error or null URI
 */
@Composable
fun rememberBitmapFromUri(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    return remember(uri) {
        if (uri == null) return@remember null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    val scale = 512f / maxOf(info.size.width, info.size.height)
                    if (scale < 1f) {
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt(),
                            (info.size.height * scale).toInt()
                        )
                    }
                }.asImageBitmap()
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri).asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Screen for registering a campsite with a two-step form flow and image selection (US 02.01.01, US 02.01.02).
 *
 * **Purpose:** allows campsite owners to enter primary details (Name, Location, Description, Capacity)
 * on Step 1, and optional details (Image selection via Photo Picker, Tags) on Step 2.
 *
 * **Design rationale:** collects state from [CampsiteFormViewModel], renders text input fields
 * across two steps ("Continue" to proceed, "Skip for Now" or "Submit" to save), displays field validation errors inline,
 * and automatically navigates back once saved.
 *
 * **Outstanding issues:** binary image files are selected locally via Content URIs and display on the current device,
 * but cross-device cloud persistence requires an ImageRepository backed by Firebase Storage.
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

    var currentStep by remember { mutableIntStateOf(1) }
    var tagsText by remember(state.tags) { mutableStateOf(state.tags.joinToString(", ")) }
    var step1Error by remember { mutableStateOf<String?>(null) }

    var selectedImageUri by remember(state.posterImageId) {
        mutableStateOf(state.posterImageId?.let { Uri.parse(it) })
    }
    val imageBitmap = rememberBitmapFromUri(selectedImageUri)
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if URI doesn't support persistent permissions
            }
        }
        selectedImageUri = uri
        viewModel.onPosterImageIdChange(uri?.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Back",
            modifier = Modifier.clickable {
                if (currentStep == 2) {
                    currentStep = 1
                } else {
                    navController.popBackStack()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (currentStep == 1) {
            Text(
                text = "Register your Campsite",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            Text("Name of Campsite", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = state.name,
                onValueChange = {
                    step1Error = null
                    viewModel.onNameChange(it)
                },
                placeholder = { Text("Type Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            Text("Location of Campsite", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = state.location,
                onValueChange = {
                    step1Error = null
                    viewModel.onLocationChange(it)
                },
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
                onValueChange = {
                    step1Error = null
                    viewModel.onCapacityChange(it)
                },
                placeholder = { Text("Type Capacity Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            step1Error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val parsedCap = state.capacity.toIntOrNull()
                    if (state.name.isBlank()) {
                        step1Error = "Please enter a campsite name."
                    } else if (parsedCap == null || parsedCap <= 0) {
                        step1Error = "Please enter a valid positive capacity number."
                    } else {
                        step1Error = null
                        currentStep = 2
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        } else {
            Text(
                text = "Register your Campsite\n(Optional)",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Insert Image Area
            Text("Insert Image (Optional)", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))

            if (imageBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFD9D9D9))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Selected campsite image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = {
                    selectedImageUri = null
                    viewModel.onPosterImageIdChange(null)
                }) {
                    Text("Remove Image", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFD9D9D9))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Upload Image",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ".PNG, .JPEG, .IMG, .JPG",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Campsite Tags
            Text("Campsite Tags (Optional)", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = tagsText,
                onValueChange = { newTagsText ->
                    tagsText = newTagsText
                    val tagsList = newTagsText.split(",").map { it.trim() }
                    viewModel.onTagsChange(tagsList)
                },
                placeholder = { Text("Type Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

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

            OutlinedButton(
                onClick = {
                    viewModel.onTagsChange(emptyList())
                    viewModel.save()
                },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for Now")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.save() },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "Saving..." else "Submit")
            }
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
