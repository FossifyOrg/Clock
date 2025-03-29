package org.fossify.clock.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView

/**
 * A simple wrapper TextView that restores the original text size
 * when view width is restored.
 */
@SuppressLint("AppCompatCustomView")
class AutoFitTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : TextView(context, attrs, defStyle) {

    private var originalTextSize: Float = textSize
    private var originalWidth: Int = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (originalWidth == 0) {
            originalWidth = w
        }

        post {
            if (w >= originalWidth && textSize != originalTextSize) {
                disableAutoSizing()
                setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize)
                enableAutoSizing()
            }
        }
    }

    private fun disableAutoSizing() {
        setAutoSizeTextTypeWithDefaults(AUTO_SIZE_TEXT_TYPE_NONE)
    }

    private fun enableAutoSizing() {
        post { setAutoSizeTextTypeWithDefaults(AUTO_SIZE_TEXT_TYPE_UNIFORM) }
    }
}
