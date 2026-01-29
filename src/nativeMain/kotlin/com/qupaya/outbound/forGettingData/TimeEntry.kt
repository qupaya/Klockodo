package com.qupaya.outbound.forGettingData

import com.qupaya.klockodo.outboundPorts.ForBuildingEntryRequests
import com.qupaya.klockodo.outboundPorts.ForGettingData
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class, ExperimentalTime::class)
@Serializable
@JsonIgnoreUnknownKeys
data class TimeEntry(
    val id: Long,
    val customers_id: Long,
    val projects_id: Long? = null,
    val services_id: Long? = null,
    val billable: Long,
    val text: String? = null,
    val duration: Long? = null,
    @Contextual
    @Serializable(with = InstantSerializer::class)
    val time_since: Instant,
    @Contextual
    @Serializable(with = InstantSerializer::class)
    val time_until: Instant? = null,
) : ForGettingData.TimeEntry {
    val computedDuration: Duration? = time_until?.minus(time_since)

    override fun getStartTime(): Instant = time_since
    override fun getDuration(): Duration? = computedDuration
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class RunningTimeEntry(
    val running: TimeEntry?,
    val stopped: TimeEntry?
)

@OptIn(ExperimentalSerializationApi::class, ExperimentalTime::class)
@Serializable
@JsonIgnoreUnknownKeys
data class Entries(
    val entries: List<TimeEntry>,
) {
    val totalDuration = entries
        .mapNotNull(TimeEntry::computedDuration)
        .plus(Duration.ZERO)
        .reduce(Duration::plus)
}


@Serializable
data class EntryRequest(
    val customers_id: Long,
    val projects_id: Long,
    val services_id: Long,
    val billable: Long,
    val text: String? = null,
) : ForBuildingEntryRequests.EntryRequest
