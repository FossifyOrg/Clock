package org.fossify.clock.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemAddTimeZoneBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.models.MyTimeZone
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor

class SelectTimeZonesAdapter(val activity: SimpleActivity, val timeZones: ArrayList<MyTimeZone>) : RecyclerView.Adapter<SelectTimeZonesAdapter.ViewHolder>() {
    private val config = activity.config
    private val textColor = activity.getProperTextColor()
    private val backgroundColor = activity.getProperBackgroundColor()
    private val primaryColor = activity.getProperPrimaryColor()
    var selectedKeys = HashSet<Int>()

    init {
        val selectedTimeZones = config.selectedTimeZones
        timeZones.forEachIndexed { index, myTimeZone ->
            if (selectedTimeZones.contains(myTimeZone.id.toString())) {
                selectedKeys.add(myTimeZone.id)
            }
        }
    }

    private fun toggleItemSelection(select: Boolean, pos: Int) {
        val itemKey = timeZones.getOrNull(pos)?.id ?: return

        if (select) {
            selectedKeys.add(itemKey)
        } else {
            selectedKeys.remove(itemKey)
        }

        notifyItemChanged(pos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemAddTimeZoneBinding.inflate(activity.layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(timeZones[position], textColor, primaryColor, backgroundColor)
    }

    override fun getItemCount() = timeZones.size

    inner class ViewHolder(private val binding: ItemAddTimeZoneBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindView(myTimeZone: MyTimeZone, textColor: Int, primaryColor: Int, backgroundColor: Int): View {
            val isSelected = selectedKeys.contains(myTimeZone.id)
            binding.apply {
                addTimeZoneCheckbox.isChecked = isSelected
                addTimeZoneTitle.text = myTimeZone.title
                addTimeZoneTitle.setTextColor(textColor)

                addTimeZoneCheckbox.setColors(textColor, primaryColor, backgroundColor)
                addTimeZoneHolder.setOnClickListener {
                    viewClicked(myTimeZone)
                }
            }

            return itemView
        }

        private fun viewClicked(myTimeZone: MyTimeZone) {
            val isSelected = selectedKeys.contains(myTimeZone.id)
            toggleItemSelection(!isSelected, adapterPosition)
        }
    }
}
