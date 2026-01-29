package com.qupaya.outbound.forGettingTime

import com.qupaya.klockodo.outboundPorts.ForGettingTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class LocalTimer : ForGettingTime {
    override fun getTime(): Instant {
        return Clock.System.now()
    }

    override fun getCurrentDate(): LocalDate {
        return Clock.System.todayIn(TimeZone.currentSystemDefault())
    }
}