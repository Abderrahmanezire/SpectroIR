package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object FileUtils {
    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) {
                            name = cursor.getString(idx)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (name.isNullOrBlank()) {
            val last = uri.lastPathSegment ?: "spectrum.csv"
            name = last.substringAfterLast('/').substringAfterLast(':')
        }
        return if (!name.isNullOrBlank()) name else "spectrum.csv"
    }
}
