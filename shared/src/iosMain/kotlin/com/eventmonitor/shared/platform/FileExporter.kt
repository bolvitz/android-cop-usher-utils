package com.eventmonitor.shared.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual class FileExporter {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun exportCsv(fileName: String, csvContent: String): Result<String> {
        return try {
            val bytes = csvContent.encodeToByteArray()
            val nsData = bytes.usePinned { pinned ->
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = bytes.size.toULong()
                )
            }

            val tempDirPath = NSTemporaryDirectory()
            val filePath = tempDirPath + fileName
            val fileURL = NSURL.fileURLWithPath(filePath)

            nsData.writeToFile(filePath, atomically = true)

            val activityViewController = UIActivityViewController(
                activityItems = listOf(fileURL),
                applicationActivities = null
            )

            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootViewController?.presentViewController(
                activityViewController,
                animated = true,
                completion = null
            )

            Result.success(filePath)
        } catch (e: Exception) {
            Result.failure(ExportError.IoError(e.message ?: "Failed to export file"))
        }
    }
}
