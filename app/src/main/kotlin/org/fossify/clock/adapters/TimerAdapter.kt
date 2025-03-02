package org.fossify.clock.adapters

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import me.grantland.widget.AutofitHelper
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemTimerBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.getFormattedDuration
import org.fossify.clock.extensions.hideTimerNotification
import org.fossify.clock.extensions.secondsToMillis
import org.fossify.clock.extensions.swap
import org.fossify.clock.models.Timer
import org.fossify.clock.models.TimerEvent
import org.fossify.clock.models.TimerState
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.adapters.MyRecyclerViewListAdapter
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beInvisibleIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getFormattedDuration
import org.fossify.commons.extensions.openNotificationSettings
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.commons.interfaces.ItemMoveCallback
import org.fossify.commons.interfaces.ItemTouchHelperContract
import org.fossify.commons.interfaces.StartReorderDragListener
import org.fossify.commons.views.MyRecyclerView
import org.greenrobot.eventbus.EventBus

class TimerAdapter(
    private val simpleActivity: SimpleActivity,
    recyclerView: MyRecyclerView,
    onRefresh: () -> Unit,
    onItemClick: (Timer) -> Unit,
) : MyRecyclerViewListAdapter<Timer>(
    activity = simpleActivity,
    recyclerView = recyclerView,
    diffUtil = diffUtil,
    itemClick = onItemClick,
    onRefresh = onRefresh
), ItemTouchHelperContract {

    private var startReorderDragListener: StartReorderDragListener

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
        setHasStableIds(true)

        val touchHelper = ItemTouchHelper(ItemMoveCallback(this))
        touchHelper.attachToRecyclerView(recyclerView)
        startReorderDragListener = object : StartReorderDragListener {
            override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id!!.toLong()
    }

    override fun submitList(list: MutableList<Timer>?, commitCallback: Runnable?) {
        val layoutManager = recyclerView.layoutManager!!
        val recyclerViewState = layoutManager.onSaveInstanceState()
        super.submitList(list) {
            layoutManager.onRestoreInstanceState(recyclerViewState)
            commitCallback?.run()
        }
    }

    override fun submitList(list: MutableList<Timer>?) {
        submitList(list, null)
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

    @SuppressLint("NotifyDataSetChanged")
    override fun onActionModeCreated() {
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActionModeDestroyed() {
        notifyDataSetChanged()
    }

    override fun onRowClear(myViewHolder: MyRecyclerViewAdapter.ViewHolder?) {}

    override fun onRowSelected(myViewHolder: MyRecyclerViewAdapter.ViewHolder?) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemTimerBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(getItem(position), true, true) { itemView, _ ->
            setupView(itemView, getItem(position), holder)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView(view: View, timer: Timer, holder: ViewHolder) {
        ItemTimerBinding.bind(view).apply {
            val isSelected = selectedKeys.contains(timer.id)
            timerFrame.isSelected = isSelected
            timerDragHandle.beVisibleIf(selectedKeys.isNotEmpty())
            timerDragHandle.applyColorFilter(textColor)
            timerDragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    startReorderDragListener.requestDrag(holder)
                }
                false
            }

            timerLabel.setTextColor(textColor)
            timerLabel.setHintTextColor(textColor.adjustAlpha(0.7f))
            timerLabel.text = timer.label
            timerLabel.beVisibleIf(timer.label.isNotEmpty())

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

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        val timers = currentList.toMutableList()
        timers.swap(fromPosition, toPosition)
        submitList(timers)
        saveAlarmsCustomOrder(ArrayList(timers))
        if (simpleActivity.config.timerSort != SORT_BY_CUSTOM) {
            simpleActivity.config.timerSort = SORT_BY_CUSTOM
        }
    }

    private fun saveAlarmsCustomOrder(alarms: ArrayList<Timer>) {
        val timersCustomSortingIds = alarms.map { it.id }
        simpleActivity.config.timersCustomSorting = timersCustomSortingIds.joinToString { it.toString() }
    }
}
