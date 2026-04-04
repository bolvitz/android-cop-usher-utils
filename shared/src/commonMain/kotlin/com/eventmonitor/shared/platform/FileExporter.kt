package com.eventmonitor.shared.platform

expect class FileExporter {
    suspend fun exportCsv(fileName: String, csvContent: String): Result<String>
}

sealed class ExportError : Exception() {
    data class IoError(override val message: String) : ExportError()
    data class PermissionDenied(override val message: String) : ExportError()
    data object Cancelled : ExportError()
}
