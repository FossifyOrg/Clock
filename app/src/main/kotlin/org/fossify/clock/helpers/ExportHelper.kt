package org.fossify.clock.helpers

import android.content.Context
import android.net.Uri
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.fossify.clock.models.AlarmTimerBackup
import org.fossify.commons.helpers.ExportResult

class ExportHelper(private val context: Context) {

    @OptIn(ExperimentalSerializationApi::class)
    fun exportData(
        backup: AlarmTimerBackup,
        outputUri: Uri?,
        callback: (result: ExportResult) -> Unit,
    ) {
        if (outputUri == null) {
            callback.invoke(ExportResult.EXPORT_FAIL)
            return
        }

        try {
            val json = Json { encodeDefaults = true }
            context.contentResolver.openOutputStream(outputUri)?.use { out ->
                json.encodeToStream(backup, out)
                callback.invoke(ExportResult.EXPORT_OK)
            } ?: throw NullPointerException("Output stream is null")
        } catch (e: Exception) {
            callback.invoke(ExportResult.EXPORT_FAIL)
        }
    }
}
