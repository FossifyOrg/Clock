package org.fossify.clock.views

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.widget.TextClock
import androidx.annotation.AttrRes
import org.fossify.clock.extensions.config
import java.text.DateFormatSymbols

private const val AM_PM_SCALE = 0.4f

class MyTextClock @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = android.R.attr.textViewStyle,
) : TextClock(context, attrs, defStyleAttr) {

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
        var index = -1
        var amPmString: String? = null
        for (s in amPmStrings) {
            if (s.isNotEmpty()) {
                val i = full.indexOf(s, ignoreCase = true)
                if (i != -1) {
                    index = i
                    amPmString = s
                    break
                }
            }
        }

        if (index != -1 && amPmString != null) {
            val spannable = SpannableString(text)
            spannable.setSpan(
                RelativeSizeSpan(AM_PM_SCALE),
                index - 1, // including the space before AM/PM
                index + amPmString.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
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
