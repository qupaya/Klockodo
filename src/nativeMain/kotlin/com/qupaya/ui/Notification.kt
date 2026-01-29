package com.qupaya.ui

import com.qupaya.APP_NAME
import com.qupaya.userHome
import klockodo.notify_init
import klockodo.notify_notification_new
import klockodo.notify_notification_show
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
object Notification {
    init {
        notify_init(APP_NAME)
    }

    fun show(message: String) {
        val notification = notify_notification_new(
            APP_NAME,
            message,
            "${userHome}/.local/share/$APP_NAME/klockodo.png"
        )
        notify_notification_show(notification, null)
    }
}