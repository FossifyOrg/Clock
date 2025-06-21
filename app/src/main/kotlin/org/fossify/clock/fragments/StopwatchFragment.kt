package org.fossify.clock.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.adapters.StopwatchAdapter
import org.fossify.clock.databinding.FragmentStopwatchBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.formatStopwatchTime
import org.fossify.clock.helpers.SORT_BY_LAP
import org.fossify.clock.helpers.SORT_BY_LAP_TIME
import org.fossify.clock.helpers.SORT_BY_TOTAL_TIME
import org.fossify.clock.helpers.STOPWATCH_LIVE_LAP_ID
import org.fossify.clock.helpers.Stopwatch
import org.fossify.clock.models.Lap
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beInvisible
import org.fossify.commons.extensions.beInvisibleIf
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.flipBit
import org.fossify.commons.extensions.getColoredBitmap
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.openNotificationSettings
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.helpers.SORT_DESCENDING

class StopwatchFragment : Fragment() {

    private var stopwatchAdapter: StopwatchAdapter? = null
    private lateinit var binding: FragmentStopwatchBinding

    private var latestLapTime: Long = 0L
    private var latestTotalTime: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
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
                scrollToTop()
            }

            stopwatchAdapter = StopwatchAdapter(
                activity = activity as SimpleActivity,
                recyclerView = stopwatchList
            ) {
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
        val properTextColor = requireContext().getProperTextColor()
        binding.apply {
            requireContext().updateTextColors(stopwatchFragment)
            stopwatchTime.setTextColor(properTextColor)
            stopwatchPlayPause.background = resources.getColoredDrawableWithColor(
                drawableId = R.drawable.circle_background_filled,
                color = properPrimaryColor
            )
            stopwatchReset.applyColorFilter(properTextColor)
            stopwatchLap.applyColorFilter(properTextColor)
        }

        stopwatchAdapter?.apply {
            updatePrimaryColor()
            updateBackgroundColor(requireContext().getProperBackgroundColor())
            updateTextColor(properTextColor)
        }
    }

    private fun updateIcons(state: Stopwatch.State) {
        val drawableId =
            if (state == Stopwatch.State.RUNNING) {
                org.fossify.commons.R.drawable.ic_pause_vector
            } else {
                org.fossify.commons.R.drawable.ic_play_vector
            }

        val iconColor =
            if (requireContext().getProperPrimaryColor() == Color.WHITE) {
                Color.BLACK
            } else {
                Color.WHITE
            }

        binding.stopwatchPlayPause.setImageDrawable(
            resources.getColoredDrawableWithColor(
                drawableId = drawableId,
                color = iconColor
            )
        )
    }

    private fun togglePlayPause() {
        (activity as SimpleActivity).handleNotificationPermission { granted ->
            if (granted) {
                Stopwatch.toggle()
            } else {
                PermissionRequiredDialog(
                    activity as SimpleActivity,
                    org.fossify.commons.R.string.allow_notifications_reminders,
                    { (activity as SimpleActivity).openNotificationSettings() })
            }
        }
    }

    private fun resetStopwatch() {
        Stopwatch.reset()
        latestLapTime = 0L
        latestTotalTime = 0L

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
        scrollToTop()
    }

    private fun updateSorting(sorting: Int) {
        updateSortingIndicators(sorting)
        Lap.sorting = sorting
        requireContext().config.stopwatchLapsSort = sorting
        updateLaps()
    }

    private fun updateSortingIndicators(sorting: Int) {
        var bitmap = requireContext().resources.getColoredBitmap(
            resourceId = R.drawable.ic_sorting_triangle_vector,
            newColor = requireContext().getProperPrimaryColor()
        )
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
                bitmap =
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            activeIndicator.setImageBitmap(bitmap)
        }
    }

    fun startStopWatch() {
        if (Stopwatch.state == Stopwatch.State.STOPPED) {
            togglePlayPause()
        }
    }

    private fun updateLaps() = lifecycleScope.launch {
        stopwatchAdapter?.submitList(
            withContext(Dispatchers.Default) {
                val laps = ArrayList(Stopwatch.laps)
                if (laps.isNotEmpty() && Stopwatch.state != Stopwatch.State.STOPPED) {
                    laps += Lap(
                        id = STOPWATCH_LIVE_LAP_ID,
                        lapTime = latestLapTime,
                        totalTime = latestTotalTime
                    )
                }
                laps.sort()
                laps
            }
        )
    }

    private fun scrollToTop() {
        binding.stopwatchList.post {
            binding.stopwatchList.scrollToPosition(0)
        }
    }

    private val updateListener = object : Stopwatch.UpdateListener {
        override fun onUpdate(totalTime: Long, lapTime: Long, useLongerMSFormat: Boolean) {
            binding.stopwatchTime.text = totalTime.formatStopwatchTime(useLongerMSFormat)
            latestLapTime = lapTime
            latestTotalTime = totalTime
            updateLaps()
        }

        override fun onStateChanged(state: Stopwatch.State) {
            updateIcons(state)
            binding.stopwatchLap.beVisibleIf(state == Stopwatch.State.RUNNING)
            binding.stopwatchReset.beVisibleIf(state != Stopwatch.State.STOPPED)
        }
    }
}
