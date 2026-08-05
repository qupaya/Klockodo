package com.qupaya.outbound.forShowingNotifications

import com.qupaya.klockodo.outboundPorts.ForShowingNotifications
import com.qupaya.ui.Notification

class NotificationAdapter : ForShowingNotifications {
    override fun show(message: String) = Notification.show(message)
}
