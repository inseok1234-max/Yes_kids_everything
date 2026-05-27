package com.ois.stickymemo.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

private const val PLACE_IMAGE_DIR = "place_images"
private const val PLACE_IMAGE_TAG = "PlaceImageStorage"

fun copyPlaceImageToInternalStorage(context: Context, sourceUri: Uri): String? {
    return runCatching {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(sourceUri)
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.takeIf { it.isNotBlank() }
            ?: "jpg"

        val imageDir = File(context.filesDir, PLACE_IMAGE_DIR).apply {
            if (!exists()) mkdirs()
        }
        val target = File(
            imageDir,
            "place_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
        )

        resolver.openInputStream(sourceUri)?.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null

        Uri.fromFile(target).toString()
    }.onFailure { error ->
        Log.w(PLACE_IMAGE_TAG, "Failed to copy place image: $sourceUri", error)
    }.getOrNull()
}

fun deletePlaceImageFromInternalStorage(context: Context, storedUri: String): Boolean {
    return runCatching {
        val imageDir = File(context.filesDir, PLACE_IMAGE_DIR).canonicalFile
        val uri = Uri.parse(storedUri)
        val file = when (uri.scheme) {
            "file" -> File(requireNotNull(uri.path)).canonicalFile
            null -> File(storedUri).canonicalFile
            else -> return false
        }

        if (!file.path.startsWith(imageDir.path) || !file.exists()) {
            return false
        }

        file.delete()
    }.onFailure { error ->
        Log.w(PLACE_IMAGE_TAG, "Failed to delete place image.", error)
    }.getOrDefault(false)
}
