package org.fossify.clock.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.clock.R
import org.fossify.clock.databinding.DialogExportAlarmsBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.ALARMS_EXPORT_EXTENSION
import java.io.File

class ExportAlarmsDialog(
    val activity: BaseSimpleActivity,
    val path: String,
    val hidePath: Boolean,
    callback: (file: File) -> Unit,
) {
    private var realPath = path.ifEmpty { activity.internalStoragePath }
    private val config = activity.config

    init {
        val view = DialogExportAlarmsBinding.inflate(activity.layoutInflater, null, false).apply {
            exportAlarmsFolder.text = activity.humanizePath(realPath)
            exportAlarmsFilename.setText("${activity.getString(R.string.export_alarms)}_${activity.getCurrentFormattedDateTime()}")

            if (hidePath) {
                exportAlarmsFolderLabel.beGone()
                exportAlarmsFolder.beGone()
            } else {
                exportAlarmsFolder.setOnClickListener {
                    FilePickerDialog(activity, realPath, false, showFAB = true) {
                        exportAlarmsFolder.text = activity.humanizePath(it)
                        realPath = it
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view.root, this, R.string.export_alarms) { alertDialog ->
                    alertDialog.showKeyboard(view.exportAlarmsFilename)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = view.exportAlarmsFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(org.fossify.commons.R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(realPath, "$filename$ALARMS_EXPORT_EXTENSION")
                                if (!hidePath && file.exists()) {
                                    activity.toast(org.fossify.commons.R.string.name_taken)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.lastAlarmsExportPath = file.absolutePath.getParentPath()
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

