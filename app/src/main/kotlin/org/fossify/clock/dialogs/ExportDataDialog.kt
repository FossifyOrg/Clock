package org.fossify.clock.dialogs

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import org.fossify.clock.R
import org.fossify.clock.databinding.DialogExportDataBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.DATA_EXPORT_EXTENSION
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getCurrentFormattedDateTime
import org.fossify.commons.extensions.getParentPath
import org.fossify.commons.extensions.humanizePath
import org.fossify.commons.extensions.internalStoragePath
import org.fossify.commons.extensions.isAValidFilename
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.value
import org.fossify.commons.helpers.ensureBackgroundThread
import java.io.File

@SuppressLint("SetTextI18n")
class ExportDataDialog(
    private val activity: BaseSimpleActivity,
    path: String,
    private val callback: (file: File) -> Unit,
) {

    companion object {
        private const val EXPORT_FILE_NAME = "alarms_and_timers"
    }

    private val realPath = path.ifEmpty { activity.internalStoragePath }
    private val config = activity.config

    init {
        val view = DialogExportDataBinding.inflate(activity.layoutInflater, null, false).apply {
            exportDataFolder.text = activity.humanizePath(realPath)
            exportDataFilename.setText("${EXPORT_FILE_NAME}_${activity.getCurrentFormattedDateTime()}")
            exportDataFolderLabel.beGone()
            exportDataFolder.beGone()
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view = view.root,
                    dialog = this,
                    titleId = R.string.settings_export_data
                ) { alertDialog ->
                    alertDialog.showKeyboard(view.exportDataFilename)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = view.exportDataFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(org.fossify.commons.R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(realPath, "$filename$DATA_EXPORT_EXTENSION")
                                if (file.exists()) {
                                    activity.toast(org.fossify.commons.R.string.name_taken)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.lastDataExportPath = file.absolutePath.getParentPath()
                                    callback(file)
                                    alertDialog.dismiss()
                                }
                            }

                            else -> activity.toast(org.fossify.commons.R.string.invalid_name)
                        }
                    }
                }
            }
    }
}

