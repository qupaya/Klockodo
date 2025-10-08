package com.qupaya.klockodo

import com.qupaya.klockodo.api.KlockodoApi
import com.qupaya.klockodo.api.EntryRequest
import com.qupaya.klockodo.api.TimeEntry
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class Klockodo(config: Configuration) {
  private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
  private val klockodoApi = KlockodoApi(config.apiKey, config.apiUser)
  private val report = klockodoApi.getUserReport(today)
  var currentEntry = klockodoApi.loadRunningTimeEntry()
    private set
  private val todaysEntries = klockodoApi.getEntriesOfDay(today)
  private var doneSecondsToday = todaysEntries.totalDuration.inWholeSeconds
  private var doneSecondsSinceStart = 0L
  private val targetSecondsDay = config.workSecondsPerDay
  private val targetSecondsFromYear = -report.diff

  fun hasRunningTimeEntry(): Boolean = currentEntry != null

  fun start() {
    currentEntry = klockodoApi.logTimeEntry(EntryRequest(
      4382930,
      3526974,
      1459279,
      0
    )).running
  }

  fun stop() {
    currentEntry?.run {
      klockodoApi.stopTimeEntry(this).stopped?.let(::updateDoneTime)
    }
  }

  fun switch() {
    val runningTimeEntry = klockodoApi.logTimeEntry(EntryRequest(
      4382930,
      3526974,
      1459279,
      0
    ))
    currentEntry = runningTimeEntry.running
    runningTimeEntry.stopped?.let(::updateDoneTime)
  }

  private fun updateDoneTime(timeEntry: TimeEntry) {
    val duration = timeEntry.run {
      duration ?: computedDuration?.inWholeSeconds ?: throw IllegalArgumentException("Unable to calculate duration of $id.")
    }
    doneSecondsToday += duration
    doneSecondsSinceStart += duration
  }

  fun getTimeToDo(): Info {
    val runningSeconds = currentEntry?.let {
      val end = Clock.System.now()
      end.epochSeconds - it.time_since.epochSeconds
    } ?: 0
    return Info(
      targetSecondsDay - doneSecondsToday - runningSeconds,
      targetSecondsFromYear - doneSecondsSinceStart - runningSeconds
    )
  }
}