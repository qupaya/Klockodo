package com.qupaya.klockodo.ui

import com.qupaya.klockodo.APP_NAME
import com.qupaya.klockodo.userHome
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import klockodo.AppIndicatorCategory
import klockodo.AppIndicatorStatus
import klockodo._GtkMenu
import klockodo.app_indicator_new
import klockodo.app_indicator_set_icon
import klockodo.app_indicator_set_menu
import klockodo.app_indicator_set_status
import klockodo.app_indicator_set_title

@OptIn(ExperimentalForeignApi::class)
object Indicator {
  @OptIn(ExperimentalForeignApi::class)
  val REF = app_indicator_new(
    "klockodo-indicator",
    "${userHome}/.local/share/Klockodo/klockodo.png",
    AppIndicatorCategory.APP_INDICATOR_CATEGORY_OTHER
  )

  fun init(menu: CValuesRef<_GtkMenu>?) {
    app_indicator_set_status(REF, AppIndicatorStatus.APP_INDICATOR_STATUS_ACTIVE)
    app_indicator_set_title(REF, APP_NAME)
    app_indicator_set_menu(REF, menu)
  }

  fun showInactive() {
    app_indicator_set_icon(REF, "${userHome}/.local/share/Klockodo/klockodo-inactive.png")
  }

  fun showActive() {
    app_indicator_set_icon(REF, "${userHome}/.local/share/Klockodo/klockodo.png")
  }

  fun showMustWork() {
    app_indicator_set_icon(REF, "${userHome}/.local/share/Klockodo/klockodo-must-work.png")
  }

  fun showMinDailyDone() {
    app_indicator_set_icon(REF, "${userHome}/.local/share/Klockodo/klockodo-min-daily-done.png")
  }

  fun showDone() {
    app_indicator_set_icon(REF, "${userHome}/.local/share/Klockodo/klockodo-done.png")
  }

  fun setIndicatorTitle(title: String) {
    app_indicator_set_title(REF, title)
  }
}