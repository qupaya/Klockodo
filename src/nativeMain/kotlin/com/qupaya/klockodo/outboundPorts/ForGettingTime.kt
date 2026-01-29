package com.qupaya.klockodo.outboundPorts

import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface ForGettingTime {
    @OptIn(ExperimentalTime::class)
    fun getTime(): Instant

    fun getCurrentDate(): LocalDate
}