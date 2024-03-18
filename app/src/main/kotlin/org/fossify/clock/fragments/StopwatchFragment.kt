package org.fossify.clock.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.adapters.StopwatchAdapter
import org.fossify.clock.databinding.FragmentStopwatchBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.formatStopwatchTime
import org.fossify.clock.helpers.SORT_BY_LAP
import org.fossify.clock.helpers.SORT_BY_LAP_TIME
import org.fossify.clock.helpers.SORT_BY_TOTAL_TIME
import org.fossify.clock.helpers.Stopwatch
import org.fossify.clock.models.Lap
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.SORT_DESCENDING

class StopwatchFragment : Fragment() {

    lateinit var stopwatchAdapter: StopwatchAdapter
    private lateinit var binding: FragmentStopwatchBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val sorting = requireContext().config.stopwatchLapsSort
        Lap.sorting = sorting
        binding = FragmentStopwatchBinding.inflate(inflater, container, false).apply {
            stopwatchTime.setOnClickListener {
                togglePlayPause()
            }

            stopwatchPlayPause.setOnClickListener {
                togglePlayPause()
            }

            stopwatchReset.setOnClickListener {
                resetStopwatch()
            }

            stopwatchSortingIndicator1.setOnClickListener {
                changeSorting(SORT_BY_LAP)
            }

            stopwatchSortingIndicator2.setOnClickListener {
                changeSorting(SORT_BY_LAP_TIME)
            }

            stopwatchSortingIndicator3.setOnClickListener {
                changeSorting(SORT_BY_TOTAL_TIME)
            }

            stopwatchLap.setOnClickListener {
                stopwatchSortingIndicatorsHolder.beVisible()
                Stopwatch.lap()
                updateLaps()
            }

            stopwatchAdapter = StopwatchAdapter(activity as SimpleActivity, ArrayList(), stopwatchList) {
                if (it is Int) {
                    changeSorting(it)
                }
            }
            stopwatchList.adapter = stopwatchAdapter
        }

        updateSortingIndicators(sorting)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setupViews()

        Stopwatch.addUpdateListener(updateListener)
        updateLaps()
        binding.stopwatchSortingIndicatorsHolder.beVisibleIf(Stopwatch.laps.isNotEmpty())
        if (Stopwatch.laps.isNotEmpty()) {
            updateSorting(Lap.sorting)
        }

        if (requireContext().config.toggleStopwatch) {
            requireContext().config.toggleStopwatch = false
            startStopWatch()
        }
    }

    override fun onPause() {
        super.onPause()
        Stopwatch.removeUpdateListener(updateListener)
    }

    private fun setupViews() {
        val properPrimaryColor = requireContext().getProperPrimaryColor()
        binding.apply {
            requireContext().updateTextColors(stopwatchFragment)
            stopwatchPlayPause.background = resources.getColoredDrawableWithColor(R.drawable.circle_background_filled, properPrimaryColor)
            stopwatchReset.applyColorFilter(requireContext().getProperTextColor())
        }
    }

    private fun updateIcons(state: Stopwatch.State) {
        val drawableId =
            if (state == Stopwatch.State.RUNNING) org.fossify.commons.R.drawable.ic_pause_vector else org.fossify.commons.R.drawable.ic_play_vector
        val iconColor = if (requireContext().getProperPrimaryColor() == Color.WHITE) Color.BLACK else Color.WHITE
        binding.stopwatchPlayPause.setImageDrawable(resources.getColoredDrawableWithColor(drawableId, iconColor))
    }

    private fun togglePlayPause() {
        (activity as SimpleActivity).handleNotificationPermission { granted ->
            if (granted) {
                Stopwatch.toggle(true)
            } else {
                PermissionRequiredDialog(
                    activity as SimpleActivity,
                    org.fossify.commons.R.string.allow_notifications_reminders,
                    { (activity as SimpleActivity).openNotificationSettings() })
            }
        }
    }

    private fun updateDisplayedText(totalTime: Long, lapTime: Long, useLongerMSFormat: Boolean) {
        binding.stopwatchTime.text = totalTime.formatStopwatchTime(useLongerMSFormat)
        if (Stopwatch.laps.isNotEmpty() && lapTime != -1L) {
            stopwatchAdapter.updateLastField(lapTime, totalTime)
        }
    }

    private fun resetStopwatch() {
        Stopwatch.reset()

        updateLaps()
        binding.apply {
            stopwatchReset.beGone()
            stopwatchLap.beGone()
            stopwatchTime.text = 0L.formatStopwatchTime(false)
            stopwatchSortingIndicatorsHolder.beInvisible()
        }
    }

    private fun changeSorting(clickedValue: Int) {
        val sorting = if (Lap.sorting and clickedValue != 0) {
            Lap.sorting.flipBit(SORT_DESCENDING)
        } else {
            clickedValue or SORT_DESCENDING
        }
        updateSorting(sorting)
    }

    private fun updateSorting(sorting: Int) {
        updateSortingIndicators(sorting)
        Lap.sorting = sorting
        requireContext().config.stopwatchLapsSort = sorting
        updateLaps()
    }

    private fun updateSortingIndicators(sorting: Int) {
        var bitmap = requireContext().resources.getColoredBitmap(R.drawable.ic_sorting_triangle_vector, requireContext().getProperPrimaryColor())
        binding.apply {
            stopwatchSortingIndicator1.beInvisibleIf(sorting and SORT_BY_LAP == 0)
            stopwatchSortingIndicator2.beInvisibleIf(sorting and SORT_BY_LAP_TIME == 0)
            stopwatchSortingIndicator3.beInvisibleIf(sorting and SORT_BY_TOTAL_TIME == 0)

            val activeIndicator = when {
                sorting and SORT_BY_LAP != 0 -> stopwatchSortingIndicator1
                sorting and SORT_BY_LAP_TIME != 0 -> stopwatchSortingIndicator2
                else -> stopwatchSortingIndicator3
            }

            if (sorting and SORT_DESCENDING == 0) {
                val matrix = Matrix()
                matrix.postScale(1f, -1f)
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            activeIndicator.setImageBitmap(bitmap)
        }
    }

    fun startStopWatch() {
        if (Stopwatch.state == Stopwatch.State.STOPPED) {
            togglePlayPause()
        }
    }

    private fun updateLaps() {
        stopwatchAdapter.apply {
            updatePrimaryColor()
            updateBackgroundColor(requireContext().getProperBackgroundColor())
            updateTextColor(requireContext().getProperTextColor())
            updateItems(Stopwatch.laps)
        }
    }

    private val updateListener = object : Stopwatch.UpdateListener {
        override fun onUpdate(totalTime: Long, lapTime: Long, useLongerMSFormat: Boolean) {
            activity?.runOnUiThread {
                updateDisplayedText(totalTime, lapTime, useLongerMSFormat)
            }
        }

        override fun onStateChanged(state: Stopwatch.State) {
            activity?.runOnUiThread {
                updateIcons(state)
                binding.stopwatchLap.beVisibleIf(state == Stopwatch.State.RUNNING)
                binding.stopwatchReset.beVisibleIf(state != Stopwatch.State.STOPPED)
            }
        }
    }
}
