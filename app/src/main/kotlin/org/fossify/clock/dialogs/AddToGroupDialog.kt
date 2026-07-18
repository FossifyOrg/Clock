package org.fossify.clock.dialogs

import android.widget.Toast
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
            mutableGroups.add(0, Group(0, 0, noneText))
        }

        mutableGroups.forEach { group ->
            val radioButton = MyCompatRadioButton(activity).apply {
                id = android.view.View.generateViewId()
                text = group.title
            }
            radioIdToGroupId[radioButton.id] = group.ref
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
        val groupNameExists = activity.dbHelper.getGroups().any { it.title == newGroupName }

        val groupId = if (newGroupName.isNotEmpty() && !groupNameExists) {
            activity.dbHelper.insertGroup(newGroupName)
        } else if (groupNameExists) {
            val text = activity.resources.getString(org.fossify.clock.R.string.group_name_exists)
            Toast.makeText(activity.baseContext, text, Toast.LENGTH_SHORT).show()
            return
        } else {
            val checkedRadioId = binding.addToGroupRadioGroup.checkedRadioButtonId
            radioIdToGroupId[checkedRadioId] ?: return
        }

        callback(groupId)
    }
}
