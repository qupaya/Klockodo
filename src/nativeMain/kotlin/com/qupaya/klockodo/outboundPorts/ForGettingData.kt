package com.qupaya.klockodo.outboundPorts

import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface ForGettingData {
    interface TimeEntry {
        @OptIn(ExperimentalTime::class)
        fun getStartTime(): Instant

        fun getDuration(): Duration?
    }

    interface StartedStoppedEntries {
        val startedEntry: TimeEntry?
        val stoppedEntry: TimeEntry?
    }

    fun fetchOpenWorkTimeOfYear(year: Int): Duration

    fun fetchWorkedTimeOfDay(day: LocalDate): Duration

    fun getCurrentTimeEntry(): TimeEntry?

    fun startTimeEntry(request: ForBuildingEntryRequests.EntryRequest): StartedStoppedEntries

    fun stopTimeEntry(entry: TimeEntry): TimeEntry?
}