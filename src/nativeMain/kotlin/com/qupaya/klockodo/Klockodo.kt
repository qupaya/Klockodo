package com.qupaya.klockodo

import com.qupaya.klockodo.inboundPorts.ForLoggingTime
import com.qupaya.klockodo.model.WorkTime
import com.qupaya.klockodo.outboundPorts.ForBuildingEntryRequests
import com.qupaya.klockodo.outboundPorts.ForGettingData
import com.qupaya.klockodo.outboundPorts.ForGettingTime
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class Klockodo(
    val workTimePerDay: Duration,
    val forGettingData: ForGettingData,
    val forGettingTime: ForGettingTime,
    val forBuildingEntryRequests: ForBuildingEntryRequests
) : ForLoggingTime {
    private val today = forGettingTime.getCurrentDate()

    private val openWorkTimeOfYear = forGettingData.fetchOpenWorkTimeOfYear(today.year)
    private val initialDoneWorkToday = forGettingData.fetchWorkedTimeOfDay(today)

    private var currentEntry = forGettingData.getCurrentTimeEntry()

    @OptIn(ExperimentalTime::class)
    private val initialTodaysRunningTime =
        currentEntry?.let { forGettingTime.getTime().minus(it.getStartTime()) } ?: Duration.ZERO

    private var todaysDoneTime = Duration.ZERO

    @OptIn(ExperimentalTime::class)
    override fun getWorkTime(): WorkTime {
        val trackedRunningTime = currentEntry?.let { forGettingTime.getTime().minus(it.getStartTime()) }
            ?: Duration.ZERO

        return WorkTime(
            today = workTimePerDay - initialDoneWorkToday - trackedRunningTime - todaysDoneTime,
            year = openWorkTimeOfYear - trackedRunningTime + initialTodaysRunningTime - todaysDoneTime
        )
    }

    override fun hasRunningLog(): Boolean {
        return currentEntry != null
    }

    override fun startLog() {
        val startedStoppedEntries = forGettingData.startTimeEntry(forBuildingEntryRequests.buildEntryRequest())
        updateTodaysTime(currentEntry, startedStoppedEntries.stoppedEntry)
        currentEntry = startedStoppedEntries.startedEntry
    }

    override fun stopLog() {
        currentEntry?.let {
            val updatedEntry = forGettingData.stopTimeEntry(it)
            updateTodaysTime(it, updatedEntry)
        }
        currentEntry = null
    }

    @OptIn(ExperimentalTime::class)
    private fun updateTodaysTime(
        lastEntryBeforeStop: ForGettingData.TimeEntry?,
        lastEntryAfterStop: ForGettingData.TimeEntry?
    ) {
        if (lastEntryAfterStop == null) {
            return
        }
        if (lastEntryBeforeStop != null && lastEntryBeforeStop.getStartTime() != lastEntryAfterStop.getStartTime()) {
            todaysDoneTime += lastEntryAfterStop.getEndTime()?.minus(lastEntryBeforeStop.getStartTime())
                ?: Duration.ZERO
        } else {
            todaysDoneTime += lastEntryAfterStop.getDuration() ?: Duration.ZERO
        }
    }
}