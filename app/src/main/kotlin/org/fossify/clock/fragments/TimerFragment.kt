package org.fossify.clock.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.adapters.TimerAdapter
import org.fossify.clock.databinding.FragmentTimerBinding
import org.fossify.clock.dialogs.ChangeTimerSortDialog
import org.fossify.clock.dialogs.EditTimerDialog
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.createNewTimer
import org.fossify.clock.extensions.timerHelper
import org.fossify.clock.helpers.DisabledItemChangeAnimator
import org.fossify.clock.helpers.SORT_BY_TIMER_DURATION
import org.fossify.clock.models.Timer
import org.fossify.clock.models.TimerEvent
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.commons.helpers.SORT_BY_DATE_CREATED
import org.fossify.commons.models.AlarmSound
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class TimerFragment : Fragment() {
    companion object {
        private const val INVALID_POSITION = -1
    }

    private lateinit var binding: FragmentTimerBinding
    private lateinit var timerAdapter: TimerAdapter
    private var timerPositionToScrollTo = INVALID_POSITION
    private var currentEditAlarmDialog: EditTimerDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentTimerBinding.inflate(inflater, container, false).apply {
            timersList.itemAnimator = DisabledItemChangeAnimator()
            timerAdd.setOnClickListener {
                activity?.run {
                    hideKeyboard()
                    openEditTimer(createNewTimer())
                }
            }
        }

        initOrUpdateAdapter()
        refreshTimers()

        // the initial timer is created asynchronously at first launch, make sure we show it once created
        if (context?.config?.appRunCount == 1) {
            Handler(Looper.getMainLooper()).postDelayed({
                refreshTimers()
            }, 1000)
        }

        return binding.root
    }

    private fun initOrUpdateAdapter() {
        if (this::timerAdapter.isInitialized) {
            timerAdapter.updatePrimaryColor()
            timerAdapter.updateBackgroundColor(requireContext().getProperBackgroundColor())
            timerAdapter.updateTextColor(requireContext().getProperTextColor())
        } else {
            timerAdapter = TimerAdapter(
                simpleActivity = requireActivity() as SimpleActivity,
                recyclerView = binding.timersList,
                onRefresh = ::refreshTimers,
                onItemClick = ::openEditTimer
            )
            binding.timersList.adapter = timerAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().updateTextColors(binding.root)
        initOrUpdateAdapter()
        refreshTimers()
    }

    fun showSortingDialog() {
        ChangeTimerSortDialog(activity as SimpleActivity) {
            refreshTimers(
                animate = false // disable sorting animations for now.
            )
        }
    }

    private fun getSortedTimers(callback: (List<Timer>) -> Unit) {
        activity?.timerHelper?.getTimers { timers ->
            val safeContext = context ?: return@getTimers
            val sortedTimers = when (safeContext.config.timerSort) {
                SORT_BY_TIMER_DURATION -> timers.sortedBy { it.seconds }
                SORT_BY_DATE_CREATED -> timers.sortedBy { it.id }
                SORT_BY_CUSTOM -> {
                    val customTimersSortOrderString = activity?.config?.timersCustomSorting
                    if (customTimersSortOrderString == "") {
                        timers.sortedBy { it.id }
                    } else {
                        val customTimersSortOrder =
                            customTimersSortOrderString?.split(", ")?.map { it.toInt() }!!
                        val timersIdValueMap = timers.associateBy { it.id }

                        val sortedTimers: ArrayList<Timer> = ArrayList()
                        customTimersSortOrder.map { id ->
                            if (timersIdValueMap[id] != null) {
                                sortedTimers.add(timersIdValueMap[id] as Timer)
                            }
                        }

                        (sortedTimers + timers.filter { it !in sortedTimers }) as ArrayList<Timer>
                    }
                }

                else -> timers
            }

            activity?.runOnUiThread {
                callback(sortedTimers)
            }
        }
    }

    private fun refreshTimers(animate: Boolean = true) {
        getSortedTimers { timers ->
            with(binding.timersList) {
                val originalAnimator = itemAnimator
                if (!animate) {
                    itemAnimator = null
                }

                timerAdapter.submitList(timers.toMutableList()) {
                    view?.post {
                        if (timerPositionToScrollTo != INVALID_POSITION &&
                            timerAdapter.itemCount > timerPositionToScrollTo
                        ) {
                            smoothScrollToPosition(timerPositionToScrollTo)
                            timerPositionToScrollTo = INVALID_POSITION
                        }

                        if (!animate) {
                            itemAnimator = originalAnimator
                        }
                    }
                }
                binding.timersPlaceholder.beVisibleIf(timers.isEmpty())
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(@Suppress("unused") event: TimerEvent.Refresh) {
        refreshTimers()
    }

    fun updateAlarmSound(alarmSound: AlarmSound) {
        currentEditAlarmDialog?.updateAlarmSound(alarmSound)
    }

    fun updatePosition(timerId: Int) {
        getSortedTimers { timers ->
            val position = timers.indexOfFirst { it.id == timerId }
            if (position != INVALID_POSITION) {
                if (timerAdapter.itemCount > position) {
                    binding.timersList.smoothScrollToPosition(position)
                } else {
                    timerPositionToScrollTo = position
                }
            }
        }
    }

    private fun openEditTimer(timer: Timer) {
        currentEditAlarmDialog = EditTimerDialog(activity as SimpleActivity, timer) {
            currentEditAlarmDialog = null
            refreshTimers()
        }
    }
}
