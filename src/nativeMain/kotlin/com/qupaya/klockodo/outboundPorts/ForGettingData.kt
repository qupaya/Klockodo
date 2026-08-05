package com.qupaya.klockodo.outboundPorts

import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface ForGettingData {
    interface TimeEntry {
        @OptIn(ExperimentalTime::class)
        fun getStartTime(): Instant

        @OptIn(ExperimentalTime::class)
        fun getEndTime(): Instant?

        fun getDuration(): Duration?
    }

    interface StartedStoppedEntries {
        val startedEntry: TimeEntry?
        val stoppedEntry: TimeEntry?
    }

    fun fetchOpenWorkTimeOfYear(year: Int): ApiResult<Duration>

    fun fetchWorkedTimeOfDay(day: LocalDate): ApiResult<Duration>

    fun getCurrentTimeEntry(): ApiResult<TimeEntry?>

    fun startTimeEntry(request: ForBuildingEntryRequests.EntryRequest): ApiResult<StartedStoppedEntries>

    fun stopTimeEntry(entry: TimeEntry): ApiResult<TimeEntry?>
}