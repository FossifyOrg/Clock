package org.fossify.clock.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.DialogRenameGroupBinding
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.models.Group
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.value

class RenameGroupDialog(
    val activity: SimpleActivity,
    val group: Group,
    val callback: () -> Unit,
) {
    private val binding = DialogRenameGroupBinding.inflate(activity.layoutInflater).apply {
        renameGroupTitle.setText(group.title)
    }

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.rename_group) { alertDialog ->
                    alertDialog.showKeyboard(binding.renameGroupTitle)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newTitle = binding.renameGroupTitle.value.trim()
                        if (newTitle.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.empty_name)
                            return@setOnClickListener
                        }

                        activity.dbHelper.updateGroupTitle(group.id, newTitle)
                        callback()
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
