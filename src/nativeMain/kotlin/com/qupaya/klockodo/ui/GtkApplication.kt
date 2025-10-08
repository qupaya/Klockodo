package com.qupaya.klockodo.ui

import klockodo.gtk_events_pending
import klockodo.gtk_init
import klockodo.gtk_main_iteration
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValuesOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalForeignApi::class)
class GtkApplication(override val coroutineContext: CoroutineContext) : CoroutineScope {
  private var stop = atomic(false)

  init {
    gtk_init(cValuesOf(0), null)
  }

  fun runMainLoop(stopFn: () -> Unit) = runBlocking {
    while(!stop.value) {
      while (gtk_events_pending() != 0) {
        gtk_main_iteration()
      }
      delay(100)
    }
    stopFn()
  }

  fun quitMainLoop() {
    stop.value = true
  }

  companion object {
    fun create(f: suspend GtkApplication.() -> Unit) = runBlocking {
      f(GtkApplication(coroutineContext))
    }
  }
}
