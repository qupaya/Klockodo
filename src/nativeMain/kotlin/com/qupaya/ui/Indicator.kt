package com.qupaya.ui

import com.qupaya.APP_NAME
import com.qupaya.userHome
import klockodo.*
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
object Indicator {
    @OptIn(ExperimentalForeignApi::class)
    val REF = app_indicator_new(
        "klockodo-indicator",
        "${userHome}/.local/share/$APP_NAME/klockodo.png",
        AppIndicatorCategory.APP_INDICATOR_CATEGORY_OTHER
    )

    fun init(menu: CValuesRef<_GtkMenu>?) {
        app_indicator_set_status(REF, AppIndicatorStatus.APP_INDICATOR_STATUS_ACTIVE)
        app_indicator_set_title(REF, APP_NAME)
        app_indicator_set_menu(REF, menu)
    }

    fun showInactive() {
        app_indicator_set_icon(REF, "${userHome}/.local/share/$APP_NAME/klockodo-inactive.png")
    }

    fun showMustWork() {
        app_indicator_set_icon(REF, "${userHome}/.local/share/$APP_NAME/klockodo-must-work.png")
    }

    fun showMinDailyDone() {
        app_indicator_set_icon(REF, "${userHome}/.local/share/$APP_NAME/klockodo-min-daily-done.png")
    }

    fun showDone() {
        app_indicator_set_icon(REF, "${userHome}/.local/share/$APP_NAME/klockodo-done.png")
    }
}