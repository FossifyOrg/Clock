package org.fossify.clock.models

import androidx.annotation.Keep
import org.json.JSONObject

@Keep
data class Alarm(
    var id: Int,
    var timeInMinutes: Int,
    var days: Int,
    var isEnabled: Boolean,
    var vibrate: Boolean,
    var soundTitle: String,
    var soundUri: String,
    var label: String,
    var oneShot: Boolean = false,
) {
    @Keep
    fun toJSON(): String {
        val jsonObject = JSONObject()
        jsonObject.put("id", id)
        jsonObject.put("timeInMinutes", timeInMinutes)
        jsonObject.put("days", days)
        jsonObject.put("isEnabled", isEnabled)
        jsonObject.put("vibrate", vibrate)
        jsonObject.put("soundTitle", soundTitle)
        jsonObject.put("soundUri", soundUri)
        jsonObject.put("label", label)
        jsonObject.put("oneShot", oneShot)
        return jsonObject.toString()
    }
}

@Keep
data class ObfuscatedAlarm(
    var a: Int,
    var b: Int,
    var c: Int,
    var d: Boolean,
    var e: Boolean,
    var f: String,
    var g: String,
    var h: String,
    var i: Boolean = false,
) {
    fun toAlarm() = Alarm(a, b, c, d, e, f, g, h, i)
}
