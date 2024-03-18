package org.fossify.clock.dialogs

import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.adapters.SelectTimeZonesAdapter
import org.fossify.clock.databinding.DialogSelectTimeZonesBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.getAllTimeZones
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff

class AddTimeZonesDialog(val activity: SimpleActivity, private val callback: () -> Unit) {
    private val binding = DialogSelectTimeZonesBinding.inflate(activity.layoutInflater)

    init {
        binding.selectTimeZonesList.adapter = SelectTimeZonesAdapter(activity, getAllTimeZones())

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun dialogConfirmed() {
        val adapter = binding.selectTimeZonesList.adapter as? SelectTimeZonesAdapter
        val selectedTimeZones = adapter?.selectedKeys?.map { it.toString() }?.toHashSet() ?: LinkedHashSet()
        activity.config.selectedTimeZones = selectedTimeZones
        callback()
    }
}
