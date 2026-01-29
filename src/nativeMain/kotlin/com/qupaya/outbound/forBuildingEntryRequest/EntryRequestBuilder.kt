package com.qupaya.outbound.forBuildingEntryRequest

import com.qupaya.klockodo.outboundPorts.ForBuildingEntryRequests
import com.qupaya.outbound.forGettingData.EntryRequest

class EntryRequestBuilder : ForBuildingEntryRequests {
    override fun buildEntryRequest(): ForBuildingEntryRequests.EntryRequest {
        return EntryRequest(
            4382930, // qupaya
            3526974, // General
            1459279, // internal
            0
        )
    }
}