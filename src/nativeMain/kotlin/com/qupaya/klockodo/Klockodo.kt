package com.qupaya.klockodo

import com.qupaya.klockodo.inboundPorts.ForLoggingTime
import com.qupaya.klockodo.model.WorkTime
import com.qupaya.klockodo.outboundPorts.ApiError
import com.qupaya.klockodo.outboundPorts.ApiResult
import com.qupaya.klockodo.outboundPorts.ForBuildingEntryRequests
import com.qupaya.klockodo.outboundPorts.ForGettingData
import com.qupaya.klockodo.outboundPorts.ForGettingTime
import com.qupaya.klockodo.outboundPorts.ForShowingNotifications
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class Klockodo(
    val workTimePerDay: Duration,
    val forGettingData: ForGettingData,
    val forGettingTime: ForGettingTime,
    val forBuildingEntryRequests: ForBuildingEntryRequests,
    val forShowingNotifications: ForShowingNotifications
) : ForLoggingTime {
    private val today = forGettingTime.getCurrentDate()

    private val openWorkTimeOfYear = forGettingData.fetchOpenWorkTimeOfYear(today.year).orNotify(Duration.ZERO)
    private val initialDoneWorkToday = forGettingData.fetchWorkedTimeOfDay(today).orNotify(Duration.ZERO)

    private var currentEntry = forGettingData.getCurrentTimeEntry().orNotify(null)

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
        when (val result = forGettingData.startTimeEntry(forBuildingEntryRequests.buildEntryRequest())) {
            is ApiResult.Failure -> forShowingNotifications.show(result.errors.toMessage())
            is ApiResult.Success -> {
                updateTodaysTime(currentEntry, result.value.stoppedEntry)
                currentEntry = result.value.startedEntry
            }
        }
    }

    override fun stopLog() {
        val entry = currentEntry ?: return
        when (val result = forGettingData.stopTimeEntry(entry)) {
            is ApiResult.Failure -> forShowingNotifications.show(result.errors.toMessage())
            is ApiResult.Success -> {
                updateTodaysTime(entry, result.value)
                currentEntry = null
            }
        }
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

    private fun <T> ApiResult<T>.orNotify(fallback: T): T = when (this) {
        is ApiResult.Success -> value
        is ApiResult.Failure -> {
            forShowingNotifications.show(errors.toMessage())
            fallback
        }
    }

    private fun List<ApiError>.toMessage(): String = joinToString("\n") { error ->
        listOfNotNull(
            error.message,
            error.path,
            error.fields?.takeIf { it.isNotEmpty() }?.joinToString(", "),
        ).joinToString("\n")
    }
}
