package org.fossify.clock.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import org.fossify.clock.databinding.WidgetConfigAnalogueBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.MyAnalogueTimeWidgetProvider
import org.fossify.commons.dialogs.ColorPickerDialog
import org.fossify.commons.dialogs.FeatureLockedDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.IS_CUSTOMIZING_COLORS

class WidgetAnalogueConfigureActivity : SimpleActivity() {
    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColor = 0
    private var mBgColorWithoutTransparency = 0
    private var mFeatureLockedDialog: FeatureLockedDialog? = null
    private val binding: WidgetConfigAnalogueBinding by viewBinding(WidgetConfigAnalogueBinding::inflate)

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(binding.root)
        initVariables()

        val isCustomizingColors = intent.extras?.getBoolean(IS_CUSTOMIZING_COLORS) ?: false
        mWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !isCustomizingColors) {
            finish()
        }

        binding.configAnalogueSave.setOnClickListener { saveConfig() }
        binding.configAnalogueSave.setTextColor(getProperPrimaryColor().getContrastColor())
        binding.configAnalogueBgColor.setOnClickListener { pickBackgroundColor() }

        val primaryColor = getProperPrimaryColor()
        binding.configAnalogueBgSeekbar.setColors(getProperTextColor(), primaryColor, primaryColor)

        if (!isCustomizingColors && !isOrWasThankYouInstalled()) {
            mFeatureLockedDialog = FeatureLockedDialog(this) {
                if (!isOrWasThankYouInstalled()) {
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mFeatureLockedDialog != null && isOrWasThankYouInstalled()) {
            mFeatureLockedDialog?.dismissDialog()
        }
    }

    private fun initVariables() {
        mBgColor = config.widgetBgColor
        if (mBgColor == resources.getColor(org.fossify.commons.R.color.default_widget_bg_color) && config.isUsingSystemTheme) {
            mBgColor = resources.getColor(org.fossify.commons.R.color.you_primary_color, theme)
        }

        mBgAlpha = Color.alpha(mBgColor) / 255.toFloat()
        mBgColorWithoutTransparency = Color.rgb(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor))

        binding.configAnalogueBgSeekbar.setOnSeekBarChangeListener(bgSeekbarChangeListener)
        binding.configAnalogueBgSeekbar.progress = (mBgAlpha * 100).toInt()
        updateBackgroundColor()
    }

    private fun saveConfig() {
        storeWidgetColors()
        requestWidgetUpdate()

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun storeWidgetColors() {
        config.apply {
            widgetBgColor = mBgColor
        }
    }

    private fun pickBackgroundColor() {
        ColorPickerDialog(this, mBgColorWithoutTransparency) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mBgColorWithoutTransparency = color
                updateBackgroundColor()
            }
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyAnalogueTimeWidgetProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateBackgroundColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        binding.configAnalogueBgColor.setFillWithStroke(mBgColor, mBgColor)
        binding.configAnalogueBackground.applyColorFilter(mBgColor)
        binding.configAnalogueSave.backgroundTintList = ColorStateList.valueOf(getProperPrimaryColor())
    }

    private val bgSeekbarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            mBgAlpha = progress.toFloat() / 100.toFloat()
            updateBackgroundColor()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }
}
