package com.campsitereservationapp.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.campsitereservationapp.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.cancellation.CancellationException

/**
 * Helpers for turning picked photos into upload-ready bytes and stored
 * images back into something Compose can draw.
 *
 * **Purpose:** shared by every screen that uploads or shows an [Image]
 * (campsite forms, owner campsite list, and later profile pictures and the
 * admin image browser).
 *
 * **Design rationale:** the Android bitmap code stays here, in the UI layer,
 * so [Image] and the ViewModels only deal with plain bytes and text and can
 * be unit tested without a phone.
 *
 * **Outstanding issues:** None.
 */
private const val TAG = "ImageBitmaps"

/** Longest side, in pixels, of an uploaded picture. */
private const val MAX_SIDE = 512

/** JPEG quality (0-100) of an uploaded picture. */
private const val JPEG_QUALITY = 80

/**
 * Reads a photo the user picked, shrinks it so its longest side is at most
 * 512px, and compresses it to JPEG. Runs off the main thread.
 *
 * @param context used to open the photo
 * @param uri the photo picked with the photo picker
 * @return the JPEG bytes, or null if the photo couldn't be read
 */
suspend fun loadJpegBytes(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val scale = MAX_SIDE.toFloat() / maxOf(info.size.width, info.size.height)
                if (scale < 1f) {
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt(),
                        (info.size.height * scale).toInt(),
                    )
                }
                // Software bitmaps can be compressed; hardware ones can't.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            val full = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            val scale = MAX_SIDE.toFloat() / maxOf(full.width, full.height)
            if (scale < 1f) {
                Bitmap.createScaledBitmap(full, (full.width * scale).toInt(), (full.height * scale).toInt(), true)
            } else {
                full
            }
        }
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Couldn't read picked photo $uri", e)
        null
    }
}

/**
 * Turns an image's stored text ([Image.data]) into a picture Compose can draw.
 * Decoded once per distinct value.
 *
 * @param data the Base64 text, or null
 * @return the picture, or null if there is none or it can't be decoded
 */
@Composable
fun rememberImageBitmap(data: String?): ImageBitmap? = remember(data) {
    val bytes = Image.decode(data) ?: return@remember null
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}
