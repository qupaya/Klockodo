package com.qupaya.toggl.ui

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import togglKlient.*

@OptIn(ExperimentalForeignApi::class)
object Menu {
  val REF = gtk_menu_new()

  fun appendMenuButton(label: String, iconName: String, callback: CPointer<CFunction<() -> Unit>>): CPointer<GtkWidget>? {
    val menuButton = gtk_image_menu_item_new_with_label(label)
    gtk_image_menu_item_set_image(
      menuButton?.reinterpret(), gtk_image_new_from_icon_name(
        iconName,
        GtkIconSize.GTK_ICON_SIZE_DIALOG
      )
    )
    g_signal_connect_data(menuButton, "activate", callback, null, null, 0u)
    gtk_widget_show(menuButton)
    gtk_menu_shell_append(REF?.reinterpret(), menuButton)

    return menuButton
  }

  fun changeMenuButtonLabelAndIcon(menuButton: CPointer<_GtkWidget>, label: String, iconName: String) {
    gtk_image_menu_item_set_image(
      menuButton.reinterpret(),
      gtk_image_new_from_icon_name(iconName, GtkIconSize.GTK_ICON_SIZE_DIALOG)
    )
    gtk_menu_item_set_label(menuButton.reinterpret(), label)
  }

  fun appendMenuLabel(label: String): CPointer<GtkWidget>? {
    val menuLabel = gtk_menu_item_new_with_label(label)
    gtk_widget_show(menuLabel)
    gtk_menu_shell_append(REF?.reinterpret(), menuLabel)
    return menuLabel
  }

  fun changeMenuLabel(menuLabel: CPointer<GtkWidget>, label: String) {
    gtk_menu_item_set_label(menuLabel.reinterpret(), label)
  }

  fun appendSeparator(): CPointer<GtkWidget>? {
    val separator = gtk_separator_menu_item_new()
    gtk_widget_show(separator)
    gtk_menu_shell_append(REF?.reinterpret(), separator)
    return separator
  }
}