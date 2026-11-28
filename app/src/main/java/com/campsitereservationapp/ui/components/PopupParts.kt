package com.campsitereservationapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.campsitereservationapp.ui.theme.CampPrimary
import com.campsitereservationapp.ui.theme.CampWhite

/**
 * A user's round profile picture, or a person icon on a green circle when they have none.
 *
 * **Purpose:** shared by the owner's reservation list and the reservation popup (mockup M34),
 * so a guest looks the same everywhere (US 02.08.01).
 *
 * **Design rationale:** takes the stored Base64 text ([com.campsitereservationapp.model.Image.data])
 * and decodes it with [rememberImageBitmap], so ViewModels only pass plain text.
 *
 * **Outstanding issues:** none.
 *
 * @param imageData the picture as Base64 JPEG text, or null for the placeholder
 * @param size diameter of the circle
 * @param modifier extra layout options
 */
@Composable
fun ProfileAvatar(imageData: String?, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = rememberImageBitmap(imageData)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(CampPrimary),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = CampWhite,
                modifier = Modifier.size(size).padding(size / 6),
            )
        }
    }
}

/**
 * A large coloured circle button with an icon, as on mockup M34.
 *
 * **Purpose:** the red and green buttons on the reservation popups (US 02.08.01, 02.11.01).
 *
 * **Design rationale:** one shared button keeps both popups matching the mockup.
 *
 * **Outstanding issues:** none.
 *
 * @param icon the icon drawn in the circle
 * @param contentDescription what the button does, for screen readers
 * @param color the circle's colour when enabled
 * @param enabled whether the button can be tapped
 * @param onClick called when tapped
 */
@Composable
fun CircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = color,
            contentColor = CampWhite,
            disabledContainerColor = color.copy(alpha = 0.35f),
            disabledContentColor = CampWhite.copy(alpha = 0.7f),
        ),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(36.dp))
    }
}

/** The red used for reject/cancel buttons on mockup M34. */
val RejectRed = Color(0xFFE53935)
