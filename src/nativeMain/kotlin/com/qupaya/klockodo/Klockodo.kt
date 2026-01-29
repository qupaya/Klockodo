package com.qupaya.klockodo

import com.qupaya.klockodo.inboundPorts.ForLoggingTime
import com.qupaya.klockodo.model.WorkTime
import com.qupaya.klockodo.outboundPorts.ForBuildingEntryRequests
import com.qupaya.klockodo.outboundPorts.ForGettingData
import com.qupaya.klockodo.outboundPorts.ForGettingTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class Klockodo(
    val workTimePerDay: Duration,
    val forGettingData: ForGettingData,
    val forGettingTime: ForGettingTime,
    val forBuildingEntryRequests: ForBuildingEntryRequests
) : ForLoggingTime {
    val mutex = Mutex()

    private val today = forGettingTime.getCurrentDate()

    private val openWorkTimeOfYear = forGettingData.fetchOpenWorkTimeOfYear(today.year)
    private val initialDoneWorkToday = forGettingData.fetchWorkedTimeOfDay(today)

    private var currentEntry = forGettingData.getCurrentTimeEntry()

    @OptIn(ExperimentalTime::class)
    private val initialTodaysRunningTime =
        currentEntry?.let { forGettingTime.getTime().minus(it.getStartTime()) } ?: Duration.ZERO

    private var todaysDoneTime = Duration.ZERO

    @OptIn(ExperimentalTime::class)
    override fun getWorkTime(): WorkTime = runBlocking {
        mutex.withLock {
            val trackedRunningTime = currentEntry?.let { forGettingTime.getTime().minus(it.getStartTime()) }
                ?: Duration.ZERO

            WorkTime(
                today = workTimePerDay - initialDoneWorkToday - trackedRunningTime - todaysDoneTime,
                year = openWorkTimeOfYear - trackedRunningTime + initialTodaysRunningTime - todaysDoneTime
            )
        }
    }

    override fun hasRunningLog(): Boolean = runBlocking {
        mutex.withLock {
            currentEntry != null
        }
    }

    override fun startLog() = runBlocking {
        mutex.withLock {
            val startedStoppedEntries = forGettingData.startTimeEntry(forBuildingEntryRequests.buildEntryRequest())
            currentEntry = startedStoppedEntries.startedEntry
            todaysDoneTime += startedStoppedEntries.stoppedEntry?.getDuration() ?: Duration.ZERO
        }
    }

    override fun stopLog() = runBlocking {
        mutex.withLock {
            currentEntry?.let {
                val updatedEntry = forGettingData.stopTimeEntry(it)
                todaysDoneTime += updatedEntry?.getDuration() ?: Duration.ZERO
            }
            currentEntry = null
        }
    }
}