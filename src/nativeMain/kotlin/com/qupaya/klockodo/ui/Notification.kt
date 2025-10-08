package com.qupaya.klockodo.ui

import com.qupaya.klockodo.APP_NAME
import com.qupaya.klockodo.userHome
import kotlinx.cinterop.ExperimentalForeignApi
import klockodo.notify_init
import klockodo.notify_notification_new
import klockodo.notify_notification_show

@OptIn(ExperimentalForeignApi::class)
object Notification {
  init {
    notify_init(APP_NAME)
  }

  fun show(message: String) {
    val notification = notify_notification_new(
      APP_NAME,
      message,
      "${userHome}/.local/share/Klockodo/klockodo.png"
    )
    notify_notification_show(notification, null)
  }
}