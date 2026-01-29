package com.qupaya.klockodo.outboundPorts

interface ForBuildingEntryRequests {
    interface EntryRequest

    fun buildEntryRequest(): EntryRequest
}