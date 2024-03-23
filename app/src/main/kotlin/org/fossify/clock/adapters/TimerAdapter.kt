package org.fossify.clock.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import me.grantland.widget.AutofitHelper
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemTimerBinding
import org.fossify.clock.extensions.getFormattedDuration
import org.fossify.clock.extensions.hideTimerNotification
import org.fossify.clock.extensions.secondsToMillis
import org.fossify.clock.models.Timer
import org.fossify.clock.models.TimerEvent
import org.fossify.clock.models.TimerState
import org.fossify.commons.adapters.MyRecyclerViewListAdapter
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.views.MyRecyclerView
import org.greenrobot.eventbus.EventBus

class TimerAdapter(
    private val simpleActivity: SimpleActivity,
    recyclerView: MyRecyclerView,
    onRefresh: () -> Unit,
    onItemClick: (Timer) -> Unit,
) : MyRecyclerViewListAdapter<Timer>(simpleActivity, recyclerView, diffUtil, onItemClick, onRefresh) {

    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<Timer>() {
            override fun areItemsTheSame(oldItem: Timer, newItem: Timer): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Timer, newItem: Timer): Boolean {
                return oldItem == newItem
            }
        }
    }

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_alarms

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_delete -> deleteItems()
        }
    }

    override fun getSelectableItemCount() = itemCount

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = getItem(position).id

    override fun getItemKeyPosition(key: Int): Int {
        var position = -1
        for (i in 0 until itemCount) {
            if (key == getItem(i).id) {
                position = i
                break
            }
        }
        return position
    }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemTimerBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(getItem(position), true, true) { itemView, _ ->
            setupView(itemView, getItem(position))
        }
        bindViewHolder(holder)
    }

    private fun deleteItems() {
        val positions = getSelectedItemPositions()
        val timersToRemove = positions.map { position ->
            getItem(position)
        }
        removeSelectedItems(positions)
        timersToRemove.forEach(::deleteTimer)
    }

    private fun setupView(view: View, timer: Timer) {
        ItemTimerBinding.bind(view).apply {
            val isSelected = selectedKeys.contains(timer.id)
            timerFrame.isSelected = isSelected

            timerLabel.setTextColor(textColor)
            timerLabel.setHintTextColor(textColor.adjustAlpha(0.7f))
            timerLabel.text = timer.label

            AutofitHelper.create(timerTime)
            timerTime.setTextColor(textColor)
            timerTime.text = when (timer.state) {
                is TimerState.Finished -> 0.getFormattedDuration()
                is TimerState.Idle -> timer.seconds.getFormattedDuration()
                is TimerState.Paused -> timer.state.tick.getFormattedDuration()
                is TimerState.Running -> timer.state.tick.getFormattedDuration()
            }

            timerReset.applyColorFilter(textColor)
            timerReset.setOnClickListener {
                resetTimer(timer)
            }

            timerPlayPause.applyColorFilter(textColor)
            timerPlayPause.setOnClickListener {
                (activity as SimpleActivity).handleNotificationPermission { granted ->
                    if (granted) {
                        when (val state = timer.state) {
                            is TimerState.Idle -> EventBus.getDefault().post(TimerEvent.Start(timer.id!!, timer.seconds.secondsToMillis))
                            is TimerState.Paused -> EventBus.getDefault().post(TimerEvent.Start(timer.id!!, state.tick))
                            is TimerState.Running -> EventBus.getDefault().post(TimerEvent.Pause(timer.id!!, state.tick))
                            is TimerState.Finished -> EventBus.getDefault().post(TimerEvent.Start(timer.id!!, timer.seconds.secondsToMillis))
                        }
                    } else {
                        PermissionRequiredDialog(
                            activity,
                            org.fossify.commons.R.string.allow_notifications_reminders,
                            { activity.openNotificationSettings() })
                    }
                }
            }

            val state = timer.state
            val resetPossible = state is TimerState.Running || state is TimerState.Paused || state is TimerState.Finished
            timerReset.beInvisibleIf(!resetPossible)
            val drawableId = if (state is TimerState.Running) {
                org.fossify.commons.R.drawable.ic_pause_vector
            } else {
                org.fossify.commons.R.drawable.ic_play_vector
            }
            timerPlayPause.setImageDrawable(simpleActivity.resources.getColoredDrawableWithColor(drawableId, textColor))
        }
    }

    private fun resetTimer(timer: Timer) {
        EventBus.getDefault().post(TimerEvent.Reset(timer.id!!))
        simpleActivity.hideTimerNotification(timer.id!!)
    }

    private fun deleteTimer(timer: Timer) {
        EventBus.getDefault().post(TimerEvent.Delete(timer.id!!))
        simpleActivity.hideTimerNotification(timer.id!!)
    }
}
