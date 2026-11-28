package com.campsitereservationapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.campsitereservationapp.ui.theme.CampSecondary

/** Colour of a filled star, matching the blue stars in the mockups. */
val StarFilled = CampSecondary

/** Colour of an empty star. */
val StarEmpty = Color(0x33000000)

/**
 * A row of five stars showing a score, with partly filled stars for
 * fractions, such as 4.3 stars (US 01.13.01, 01.13.02).
 *
 * **Purpose:** one look for star scores on the campsite details screen,
 * the reviews list and the admin ratings list.
 *
 * **Design rationale:** only uses the core star icon: an empty star with a
 * filled star clipped to the score's fraction drawn on top, so no extra icon
 * library is needed.
 *
 * **Outstanding issues:** None.
 *
 * @param score the score to show, from 0 to 5
 * @param modifier modifier for the row
 * @param starSize the size of each star
 */
@Composable
fun StarRow(score: Float, modifier: Modifier = Modifier, starSize: Dp = 20.dp) {
    Row(modifier) {
        for (i in 0 until 5) {
            val fill = (score - i).coerceIn(0f, 1f)
            Box(Modifier.size(starSize)) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = StarEmpty, modifier = Modifier.size(starSize))
                if (fill > 0f) {
                    Box(Modifier.width(starSize * fill).clipToBounds()) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = StarFilled,
                            modifier = Modifier
                                .wrapContentWidth(Alignment.Start, unbounded = true)
                                .size(starSize),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Five tappable stars for picking a whole-star score from 1 to 5, like a
 * product review (US 01.13.01, 01.13.02).
 *
 * **Purpose:** lets an entrant rate a campsite with one tap.
 *
 * **Design rationale:** stateless: the caller holds the score, so the
 * dialog can enable its Submit button once a star is picked.
 *
 * **Outstanding issues:** None.
 *
 * @param score the picked score, or 0 if none yet
 * @param onScoreChange called with 1 to 5 when a star is tapped
 * @param modifier modifier for the row
 * @param starSize the size of each star
 */
@Composable
fun StarPicker(
    score: Int,
    onScoreChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 40.dp,
) {
    Row(modifier) {
        for (star in 1..5) {
            IconButton(onClick = { onScoreChange(star) }, modifier = Modifier.size(starSize + 8.dp)) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "$star star" + if (star == 1) "" else "s",
                    tint = if (star <= score) StarFilled else StarEmpty,
                    modifier = Modifier.size(starSize),
                )
            }
        }
    }
}

/**
 * Formats a score for display: 4.0 as "4" and 4.1228 as "4.1".
 *
 * @param score the score to format
 * @return the score with at most one decimal place
 */
fun formatScore(score: Float): String {
    val rounded = Math.round(score * 10f) / 10f
    return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
}
