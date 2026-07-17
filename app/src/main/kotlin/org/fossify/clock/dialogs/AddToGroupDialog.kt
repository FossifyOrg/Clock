package org.fossify.clock.dialogs

import androidx.core.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.DialogAddToGroupBinding
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.models.Group
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.value
import org.fossify.commons.views.MyCompatRadioButton

class AddToGroupDialog(
    val activity: SimpleActivity,
    val callback: (groupId: Int) -> Unit,
) {
    private val binding = DialogAddToGroupBinding.inflate(activity.layoutInflater)
    private val groups = activity.dbHelper.getGroups()
    private val radioIdToGroupId = HashMap<Int, Int>()

    init {
        val mutableGroups = groups.toMutableList()

        if (groups.any()){
            val noneText = activity.resources.getString(org.fossify.commons.R.string.none)
            mutableGroups.add(0, Group(0, noneText))
        }

        mutableGroups.forEach { group ->
            val radioButton = MyCompatRadioButton(activity).apply {
                id = android.view.View.generateViewId()
                text = group.title
            }
            radioIdToGroupId[radioButton.id] = group.id
            binding.addToGroupRadioGroup.addView(radioButton)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { _, _ -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, org.fossify.clock.R.string.add_to_group)
            }
    }

    private fun dialogConfirmed() {
        val newGroupName = binding.addToGroupNewName.value.trim()
        val groupId = if (newGroupName.isNotEmpty()) {
            activity.dbHelper.insertGroup(newGroupName)
        } else {
            val checkedRadioId = binding.addToGroupRadioGroup.checkedRadioButtonId
            radioIdToGroupId[checkedRadioId] ?: return
        }

        callback(groupId)
    }
}
