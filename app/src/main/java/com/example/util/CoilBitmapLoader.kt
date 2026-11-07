package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CoilBitmapLoader(private val context: Context) : BitmapLoader {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun supportsMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                val sampleSize = if (maxDim > 512) Integer.highestOneBit((maxDim / 512).coerceAtLeast(1)) else 1
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize.coerceAtLeast(1)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, decodeOpts)
                if (bitmap != null) {
                    future.set(bitmap)
                } else {
                    future.setException(Exception("Could not decode bitmap"))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            try {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(320) // Use 320 to avoid TransactionTooLargeException in Samsung Quick Settings IPC
                    .allowHardware(false) // Hardware bitmaps cannot be used in notifications
                    .build()
                
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        future.set(bitmap)
                    } else {
                        future.setException(Exception("Not a bitmap drawable"))
                    }
                } else {
                    val defaultBitmap = BitmapFactory.decodeResource(context.resources, com.example.R.drawable.img_default_album_art)
                    if (defaultBitmap != null) {
                        future.set(defaultBitmap)
                    } else {
                        future.setException(Exception("Failed to load image"))
                    }
                }
            } catch (e: Exception) {
                val defaultBitmap = try {
                    BitmapFactory.decodeResource(context.resources, com.example.R.drawable.img_default_album_art)
                } catch (_: Exception) { null }
                if (defaultBitmap != null) {
                    future.set(defaultBitmap)
                } else {
                    future.setException(e)
                }
            }
        }
        return future
    }
}
