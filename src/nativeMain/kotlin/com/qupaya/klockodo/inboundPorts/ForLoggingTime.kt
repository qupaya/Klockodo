package com.qupaya.klockodo.inboundPorts

import com.qupaya.klockodo.model.WorkTime

interface ForLoggingTime {
    fun getWorkTime(): WorkTime

    fun hasRunningLog(): Boolean

    fun startLog()
    fun stopLog()
}