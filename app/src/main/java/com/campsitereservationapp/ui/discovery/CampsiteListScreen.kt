package com.campsitereservationapp.ui.discovery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campsitereservationapp.data.CampsiteFilter
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.ui.components.StarRow
import com.campsitereservationapp.ui.components.rememberImageBitmap
import com.campsitereservationapp.ui.theme.CampBackground
import com.campsitereservationapp.ui.theme.CampPrimary
import com.campsitereservationapp.ui.theme.CampSecondary
import com.campsitereservationapp.ui.theme.CampTertiary
import com.campsitereservationapp.ui.theme.CampText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// Short date for the filter panel, like 9/13/26.
private val shortDate = DateTimeFormatter.ofPattern("M/d/yy")

/**
 * The entrant's campsite search screen, styled after the search mockup
 * (US 01.01.01, 01.01.03, 01.13.01).
 *
 * **Purpose:** lets entrants search campsites by name and filter them by
 * guest capacity, location and available dates, then open a campsite's details.
 *
 * **Design rationale:** a green page with a rounded search bar, a collapsible
 * cream filter panel, and one cream card per campsite showing its poster,
 * stars, location and capacity. All filtering happens in [CampsiteListViewModel];
 * this screen only draws its state and passes user input back.
 *
 * **Outstanding issues:** the capacity slider's upper bound (20) is a guess
 * until the team settles on a maximum campsite capacity.
 *
 * @param navController navigation controller used for screen transitions
 * @param viewModel view model holding the filtered campsites, ratings and posters
 */
@Composable
fun CampsiteListScreen(
    navController: NavController,
    viewModel: CampsiteListViewModel = viewModel(factory = CampsiteListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    var displayFilters by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CampPrimary)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                SearchBar(
                    text = state.campsiteFilter.searchText,
                    onTextChange = viewModel::onFilterName,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Column {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { displayFilters = !displayFilters }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Filters", color = Color.White, fontSize = 13.sp)
                    Icon(
                        if (displayFilters) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (displayFilters) "Hide filters" else "Show filters",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (displayFilters) {
                    Spacer(Modifier.height(4.dp))
                    FilterPanel(
                        filter = state.campsiteFilter,
                        onOccupancyChange = { viewModel.onFilterOccupancy(it.toString()) },
                        onLocationChange = viewModel::onFilterLocation,
                        onPickDates = { showDatePicker = true },
                        onClearDates = { viewModel.onFilterDateRange(null, null) },
                    )
                }
            }
        }

        if (state.campsites.isEmpty() && !state.isLoading) {
            item {
                Text(
                    "No campsites match your search.",
                    color = Color.White,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        items(state.campsites, key = { it.id }) { campsite ->
            CampsiteSummaryCard(
                campsite = campsite,
                rating = state.campsiteRatings[campsite.id],
                poster = state.posters[campsite.id],
                onClickDetails = {
                    navController.navigate("EntrantCampsiteDetails/${campsite.id}")
                },
            )
        }
    }

    if (showDatePicker) {
        DateFilterMenu(
            onConfirm = viewModel::onFilterDateRange,
            onDismiss = { showDatePicker = false },
        )
    }
}

/**
 * Rounded grey search bar with a search icon and a clear button.
 *
 * @param text the current search text
 * @param onTextChange called as the user types or clears
 * @param modifier modifier for the bar
 */
@Composable
private fun SearchBar(text: String, onTextChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFDADADA))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF8A8A8A))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text("Search campsites", color = Color(0xFF8A8A8A), fontSize = 18.sp)
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 18.sp, color = CampText),
                cursorBrush = SolidColor(CampText),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (text.isNotEmpty()) {
            IconButton(onClick = { onTextChange("") }) {
                Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = Color(0xFF8A8A8A))
            }
        }
    }
}

/**
 * Cream panel with the guest capacity, location and available dates filters.
 *
 * @param filter the filter currently applied
 * @param onOccupancyChange called with the new minimum guest capacity
 * @param onLocationChange called as the location is typed or cleared
 * @param onPickDates opens the date range picker
 * @param onClearDates removes the date filter
 */
@Composable
private fun FilterPanel(
    filter: CampsiteFilter,
    onOccupancyChange: (Int) -> Unit,
    onLocationChange: (String) -> Unit,
    onPickDates: () -> Unit,
    onClearDates: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CampBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Guest Capacity", fontSize = 14.sp, modifier = Modifier.width(110.dp))
            Text(
                if (filter.occupancy > 0) filter.occupancy.toString() else "Any",
                fontSize = 14.sp,
                modifier = Modifier.width(32.dp),
            )
            Slider(
                value = filter.occupancy.toFloat(),
                onValueChange = { onOccupancyChange(it.roundToInt()) },
                valueRange = 0f..20f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = CampSecondary,
                    activeTrackColor = CampSecondary,
                    inactiveTrackColor = CampTertiary,
                ),
                modifier = Modifier.weight(1f),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Location", fontSize = 14.sp, modifier = Modifier.width(110.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CampTertiary)
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (filter.location.isEmpty()) {
                        Text("Anywhere", fontSize = 14.sp, color = Color(0xFF6B6B6B))
                    }
                    BasicTextField(
                        value = filter.location,
                        onValueChange = onLocationChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = CampText),
                        cursorBrush = SolidColor(CampText),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (filter.location.isNotEmpty()) {
                    IconButton(onClick = { onLocationChange("") }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear location",
                            tint = Color(0xFF6B6B6B),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Available Dates", fontSize = 14.sp, modifier = Modifier.width(110.dp))
            val range = filter.dateRange
            Text(
                range?.let { "${it.start.format(shortDate)}, ${it.finish.format(shortDate)}" } ?: "Any",
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            if (range != null) {
                IconButton(onClick = onClearDates, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear dates", modifier = Modifier.size(16.dp))
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPickDates)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Select", fontSize = 12.sp)
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * One campsite in the search results: name, poster, stars, location,
 * capacity and a Details button, as in the search mockup.
 *
 * @param campsite the campsite to show
 * @param rating its average star score, or null if nobody has rated it
 * @param poster its poster as Base64 JPEG text, or null if it has none
 * @param onClickDetails opens the campsite's details screen
 */
@Composable
fun CampsiteSummaryCard(
    campsite: Campsite,
    rating: Float?,
    poster: String? = null,
    onClickDetails: () -> Unit,
) {
    val bitmap = rememberImageBitmap(poster)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CampBackground)
            .padding(12.dp),
    ) {
        Text(
            campsite.name,
            fontSize = 24.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF8A9A86)),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = campsite.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("No photo yet", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (rating != null) {
                    StarRow(score = rating, starSize = 26.dp)
                } else {
                    Text("No ratings yet", fontSize = 13.sp, color = Color(0xFF6B6B6B))
                }
                Spacer(Modifier.height(4.dp))
                if (campsite.location.isNotBlank()) {
                    Text(campsite.location, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("Capacity: ${campsite.capacity}", fontSize = 15.sp)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onClickDetails,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CampSecondary, contentColor = CampText),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("Details", fontSize = 18.sp)
            }
        }
    }
}

/**
 * Date range picker popup for the available dates filter.
 *
 * @param onConfirm called with the picked start and finish dates (either may be null)
 * @param onDismiss closes the popup
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterMenu(
    onConfirm: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    pickerState.selectedStartDateMillis?.toLocalDate(),
                    pickerState.selectedEndDateMillis?.toLocalDate(),
                )
                onDismiss()
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DateRangePicker(state = pickerState)
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate() //Currently uses UTC, not sure if this needs to be changed
