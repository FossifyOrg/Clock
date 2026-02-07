package org.fossify.clock.views

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.widget.TextClock
import androidx.annotation.AttrRes
import org.fossify.clock.extensions.config
import org.fossify.commons.extensions.applyFontToTextView
import java.text.DateFormatSymbols

private const val AM_PM_SCALE = 0.4f

class MyTextClock @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = android.R.attr.textViewStyle,
) : TextClock(context, attrs, defStyleAttr) {

    init {
        if (!isInEditMode) context.applyFontToTextView(this)
    }

    private val amPmStrings by lazy {
        DateFormatSymbols.getInstance(
            resources.configuration.locales[0]
        ).amPmStrings
    }

    private var reenter = false

    override fun setText(text: CharSequence?, type: BufferType?) {
        if (reenter) {
            super.setText(text, type)
            return
        }

        if (context.config.use24HourFormat || text.isNullOrEmpty()) {
            super.setText(text, type)
            return
        }

        val full = text.toString()
        var amPmPosition = -1
        var amPmString: String? = null
        for (s in amPmStrings) {
            if (s.isNotEmpty()) {
                val i = full.indexOf(s, ignoreCase = true)
                if (i != -1) {
                    amPmPosition = i
                    amPmString = s
                    break
                }
            }
        }

        if (amPmPosition != -1 && amPmString != null) {
            val spannable = SpannableString(text)
            val startIndex = if (amPmPosition > 0 && full[amPmPosition - 1].isWhitespace()) {
                amPmPosition - 1
            } else {
                amPmPosition
            }
            val endIndex = amPmPosition + amPmString.length
            if (startIndex < endIndex) {
                spannable.setSpan(
                    RelativeSizeSpan(AM_PM_SCALE),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            reenter = true

            try {
                super.setText(spannable, type ?: BufferType.SPANNABLE)
            } finally {
                reenter = false
            }
        } else {
            super.setText(text, type)
        }
    }
}
