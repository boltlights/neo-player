package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Size
import okio.Buffer
import okio.buffer
import okio.source
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object AudioThumbnailCache {
    /**
     * Converts any raw URI string or identifier into a safe, flat, collision-free MD5 hex string.
     * Guaranteed to never contain slashes, colons, or invalid path characters.
     */
    fun toSafeKey(raw: String): String {
        val normalized = if (raw.startsWith("audiocover://")) {
            raw.replaceFirst("audiocover://", "content://")
        } else {
            raw
        }
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(normalized.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            normalized.hashCode().toString()
        }
    }

    fun getThumbnailDir(context: Context): File {
        val dir = File(context.filesDir, "thumbnails")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getThumbnailFile(context: Context, rawKey: String): File {
        val safeKey = toSafeKey(rawKey)
        return File(getThumbnailDir(context), "$safeKey.jpg")
    }

    fun hasThumbnail(context: Context, rawKey: String): Boolean {
        val file = getThumbnailFile(context, rawKey)
        return file.exists() && file.length() > 0
    }

    fun saveThumbnail(context: Context, rawKey: String, pictureBytes: ByteArray): Boolean {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size, boundsOpts)
            val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
            val sampleSize = if (maxDim > 200) maxDim / 200 else 1
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size, decodeOpts) ?: return false
            val file = getThumbnailFile(context, rawKey)
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "${file.nameWithoutExtension}.tmp")
            FileOutputStream(tmp).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
            tmp.renameTo(file)
        } catch (t: Throwable) {
            Timber.tag("AudioThumbnailCache").w(t, "Failed to save thumbnail for %s", rawKey)
            false
        }
    }
}

/**
 * Dual-tier album art fetcher ensuring:
 * 1. Buttery smooth 120 FPS list scrolling: Fast disk-cached 180px thumbnails bypass MediaMetadataRetriever completely.
 * 2. Focused Player pristine quality: Native embedded resolution, bounded to safe 2048px maximum to protect against
 *    Android RecordingCanvas limits (prevents "Canvas: trying to draw too large bitmap" fatal crashes and LMK exits).
 */
class AudioAlbumArtFetcher(
    private val data: Uri,
    private val context: Context,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val isOriginal = options.size == Size.ORIGINAL
        val rawKey = data.toString()
        val safeKey = AudioThumbnailCache.toSafeKey(rawKey)

        // 1. LIST SCROLLING TIER (180px downscaled disk thumbnail):
        if (!isOriginal && AudioThumbnailCache.hasThumbnail(context, rawKey)) {
            val thumbFile = AudioThumbnailCache.getThumbnailFile(context, rawKey)
            return SourceResult(
                source = ImageSource(thumbFile.source().buffer(), context),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK
            )
        }

        // 2. FOCUSED PLAYER TIER (High-resolution art bounded to safe max canvas size):
        val fullArtDir = File(context.cacheDir, "full_art")
        if (!fullArtDir.exists()) fullArtDir.mkdirs()
        val fullArtFile = File(fullArtDir, "$safeKey.raw")

        if (isOriginal && fullArtFile.exists() && fullArtFile.length() > 0) {
            return SourceResult(
                source = ImageSource(fullArtFile.source().buffer(), context),
                mimeType = null, // Let Coil sniff the MIME type (lossless PNG, JPEG, WEBP)
                dataSource = DataSource.DISK
            )
        }

        // 3. EXTRACTION TIER (Background worker or initial cache miss):
        val retriever = MediaMetadataRetriever()
        var pfd: ParcelFileDescriptor? = null
        try {
            val contentUri = if (data.scheme == "audiocover") {
                Uri.parse(data.toString().replaceFirst("audiocover://", "content://"))
            } else {
                data
            }

            if (contentUri.scheme == "file") {
                retriever.setDataSource(contentUri.path)
            } else if (contentUri.scheme == "content") {
                try {
                    pfd = context.contentResolver.openFileDescriptor(contentUri, "r")
                    if (pfd != null) {
                        retriever.setDataSource(pfd.fileDescriptor)
                    } else {
                        retriever.setDataSource(context, contentUri)
                    }
                } catch (_: Exception) {
                    retriever.setDataSource(context, contentUri)
                }
            } else {
                retriever.setDataSource(context, contentUri)
            }

            val picture = retriever.embeddedPicture
            if (picture != null) {
                // Safeguard against: java.lang.RuntimeException: Canvas: trying to draw too large(222010000bytes) bitmap.
                // Inspect bounds before delivering to Coil
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(picture, 0, picture.size, boundsOpts)
                val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)

                if (isOriginal) {
                    // If image is extraordinarily huge (> 2048px, e.g. 7500x7500 which is 222MB in ARGB_8888),
                    // downsample it to safe 2048px studio bounds so it does NOT exceed Android's 100MB RecordingCanvas limit
                    val safeBytes: ByteArray = if (maxDim > 2048) {
                        val sampleSize = maxDim / 2048
                        val decodeOpts = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize.coerceAtLeast(1)
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val safeBitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size, decodeOpts)
                        if (safeBitmap != null) {
                            val stream = ByteArrayOutputStream()
                            safeBitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                            safeBitmap.recycle()
                            stream.toByteArray()
                        } else {
                            picture
                        }
                    } else {
                        picture
                    }

                    try {
                        val tmpFull = File(fullArtDir, "$safeKey.tmp")
                        tmpFull.writeBytes(safeBytes)
                        tmpFull.renameTo(fullArtFile)
                    } catch (e: Exception) {
                        Timber.tag("AudioAlbumArtFetcher").w(e, "Error caching full art for %s", safeKey)
                    }

                    return SourceResult(
                        source = ImageSource(Buffer().write(safeBytes), context),
                        mimeType = null,
                        dataSource = DataSource.DISK
                    )
                } else {
                    // Pre-generate downscaled thumbnail for list scrolling
                    AudioThumbnailCache.saveThumbnail(context, rawKey, picture)
                    val thumbFile = AudioThumbnailCache.getThumbnailFile(context, rawKey)
                    if (thumbFile.exists() && thumbFile.length() > 0) {
                        return SourceResult(
                            source = ImageSource(thumbFile.source().buffer(), context),
                            mimeType = "image/jpeg",
                            dataSource = DataSource.DISK
                        )
                    }

                    return SourceResult(
                        source = ImageSource(Buffer().write(picture), context),
                        mimeType = null,
                        dataSource = DataSource.DISK
                    )
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Timber.tag("AudioAlbumArtFetcher").w(e, "Failed to retrieve album art for %s", data)
        } finally {
            try { pfd?.close() } catch (_: Exception) {}
            try { retriever.release() } catch (_: Exception) {}
        }
        return null
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val scheme = data.scheme
            if (scheme == "audiocover") {
                val realUri = Uri.parse(data.toString().replaceFirst("audiocover://", "content://"))
                return AudioAlbumArtFetcher(realUri, context, options)
            } else if (scheme == "content" && (data.toString().contains("audio") || data.toString().contains("media"))) {
                return AudioAlbumArtFetcher(data, context, options)
            } else if (scheme == "file") {
                val path = data.path?.lowercase() ?: ""
                if (path.endsWith(".mp3") || path.endsWith(".flac") || path.endsWith(".m4a") ||
                    path.endsWith(".ogg") || path.endsWith(".wav") || path.endsWith(".opus") || path.endsWith(".aac")) {
                    return AudioAlbumArtFetcher(data, context, options)
                }
            }
            return null
        }
    }
}
