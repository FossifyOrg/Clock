package org.fossify.clock.activities

import android.content.Intent
import org.fossify.clock.helpers.*
import org.fossify.commons.activities.BaseSplashActivity

class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        when {
            intent?.action == "android.intent.action.SHOW_ALARMS" -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(OPEN_TAB, TAB_ALARM)
                    startActivity(this)
                }
            }

            intent?.action == "android.intent.action.SHOW_TIMERS" -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(OPEN_TAB, TAB_TIMER)
                    startActivity(this)
                }
            }

            intent?.action == STOPWATCH_TOGGLE_ACTION -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(OPEN_TAB, TAB_STOPWATCH)
                    putExtra(TOGGLE_STOPWATCH, intent.getBooleanExtra(TOGGLE_STOPWATCH, false))
                    startActivity(this)
                }
            }

            intent.extras?.containsKey(OPEN_TAB) == true -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(OPEN_TAB, intent.getIntExtra(OPEN_TAB, TAB_CLOCK))
                    putExtra(TIMER_ID, intent.getIntExtra(TIMER_ID, INVALID_TIMER_ID))
                    startActivity(this)
                }
            }

            IntentHandlerActivity.HANDLED_ACTIONS.contains(intent?.action) -> {
                Intent(intent).apply {
                    setClass(this@SplashActivity, IntentHandlerActivity::class.java)
                    startActivity(this)
                }
            }

            else -> startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
