package com.eventmonitor.shared.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

actual class FileExporter(private val context: Context) {
    actual suspend fun exportCsv(fileName: String, csvContent: String): Result<String> {
        return try {
            val file = File(context.cacheDir, fileName)
            file.writeText(csvContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Export CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            Result.success(file.absolutePath)
        } catch (e: IOException) {
            Result.failure(ExportError.IoError(e.message ?: "Failed to write file"))
        } catch (e: Exception) {
            Result.failure(ExportError.IoError(e.message ?: "Unknown error"))
        }
    }
}
