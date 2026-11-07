package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import java.io.File

@Composable
fun AlbumArtworkFiller(
    trackTitle: String = "",
    accentColor: Color = Color.Gray,
    modifier: Modifier = Modifier,
    alpha: Float = 0.2f
) {
    Box(
        modifier = modifier
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_default_album_art),
            contentDescription = if (trackTitle.isNotEmpty()) "$trackTitle default art" else "Default album art",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * High-performance artwork loader for lists, thumbnails, and general views.
 * When [sizeLimit] is provided (e.g. 180px in song list rows), it downsamples specifically
 * for the thumbnail with a dedicated cache key to guarantee buttery smooth 60/120fps scrolling.
 * When [sizeLimit] is null, it loads high resolution artwork safely bounded to 2048px to prevent Canvas OOM.
 */
@Composable
fun TrackArtworkWidget(
    artPath: String?,
    trackTitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    sizeLimit: Int? = null
) {
    Box(modifier = modifier) {
        val isValidArt = !artPath.isNullOrBlank()
        
        if (isValidArt) {
            val context = LocalContext.current
            
            val request = remember(artPath, sizeLimit) {
                val uri = if (artPath!!.startsWith("content://") || artPath.startsWith("audiocover://") || artPath.startsWith("file://")) {
                    android.net.Uri.parse(artPath)
                } else {
                    android.net.Uri.fromFile(File(artPath))
                }
                val builder = ImageRequest.Builder(context).data(uri)
                
                if (sizeLimit != null) {
                    builder
                        .size(sizeLimit)
                        .crossfade(false) // Disabled for scrolling lists to eliminate animation jank
                        .memoryCacheKey("art_thumb_${sizeLimit}_$artPath")
                        .diskCacheKey("art_thumb_${sizeLimit}_$artPath")
                } else {
                    builder
                        .size(2048, 2048)
                        .precision(Precision.AUTOMATIC)
                        .allowHardware(true)
                        .bitmapConfig(Bitmap.Config.ARGB_8888)
                        .memoryCacheKey("art_full_$artPath")
                        .diskCacheKey("art_full_$artPath")
                }
                builder.build()
            }

            AsyncImage(
                model = request,
                contentDescription = "$trackTitle album art",
                placeholder = painterResource(id = R.drawable.img_default_album_art),
                error = painterResource(id = R.drawable.img_default_album_art),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AlbumArtworkFiller(trackTitle = trackTitle, accentColor = accentColor, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * Dedicated high-fidelity artwork loader for the Focused Now Playing Screen.
 * Uses 32-bit ARGB_8888 color, safely bounded to a maximum of 2048x2048 pixels to ensure
 * pristine, razor-sharp visual fidelity while strictly avoiding Android Canvas bitmap limits
 * (which crash with "Canvas: trying to draw too large bitmap" if unbound).
 */
@Composable
fun FocusedPlayerArtworkWidget(
    artPath: String?,
    trackTitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val isValidArt = !artPath.isNullOrBlank()
        
        if (isValidArt) {
            val context = LocalContext.current
            // Decode at the display's true pixel resolution: the artwork can never be shown larger than
            // the screen's short side, so this is the sharpest image the panel can physically show.
            // (Small headroom for the 1.05x "pulse" scale animation.)
            val targetPx = remember {
                val dm = context.resources.displayMetrics
                (minOf(dm.widthPixels, dm.heightPixels) * 1.1f).toInt().coerceIn(1024, 3072)
            }

            val request = remember(artPath, targetPx) {
                val uri = if (artPath!!.startsWith("content://") || artPath.startsWith("audiocover://") || artPath.startsWith("file://")) {
                    android.net.Uri.parse(artPath)
                } else {
                    android.net.Uri.fromFile(File(artPath))
                }
                ImageRequest.Builder(context)
                    .data(uri)
                    .size(targetPx, targetPx)
                    // INEXACT: downscale large art to fit, but never upscale small art into a huge bitmap.
                    .precision(Precision.INEXACT)
                    .allowHardware(true)
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .crossfade(200)
                    // Show the already-cached list thumbnail instantly while the full-res art decodes.
                    .placeholderMemoryCacheKey("art_thumb_180_$artPath")
                    // Fallback when no thumbnail is cached. Set on the request (not AsyncImage's
                    // `placeholder` param) because that param would override the thumbnail placeholder.
                    .placeholder(R.drawable.img_default_album_art)
                    .memoryCacheKey("art_full_${targetPx}_$artPath")
                    .diskCacheKey("art_full_${targetPx}_$artPath")
                    .build()
            }

            AsyncImage(
                model = request,
                contentDescription = "$trackTitle album art",
                error = painterResource(id = R.drawable.img_default_album_art),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AlbumArtworkFiller(trackTitle = trackTitle, accentColor = accentColor, modifier = Modifier.fillMaxSize())
        }
    }
}
