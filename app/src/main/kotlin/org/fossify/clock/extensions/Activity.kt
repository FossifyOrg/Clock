package org.fossify.clock.extensions

import org.fossify.clock.BuildConfig
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.canUseFullScreenIntent
import org.fossify.commons.extensions.openFullScreenIntentSettings
import org.fossify.commons.extensions.openNotificationSettings

fun BaseSimpleActivity.handleFullScreenNotificationsPermission(
    notificationsCallback: (granted: Boolean) -> Unit,
) {
    handleNotificationPermission { granted ->
        if (granted) {
            if (canUseFullScreenIntent()) {
                notificationsCallback(true)
            } else {
                PermissionRequiredDialog(
                    activity = this,
                    textId = org.fossify.commons.R.string.allow_full_screen_notifications_reminders,
                    positiveActionCallback = {
                        openFullScreenIntentSettings(BuildConfig.APPLICATION_ID)
                    },
                    negativeActionCallback = {
                        notificationsCallback(false)
                    }
                )
            }
        } else {
            PermissionRequiredDialog(
                activity = this,
                textId = org.fossify.commons.R.string.allow_notifications_reminders,
                positiveActionCallback = {
                    openNotificationSettings()
                },
                negativeActionCallback = {
                    notificationsCallback(false)
                }
            )
        }
    }
}
