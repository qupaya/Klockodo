package com.qupaya.outbound.forGettingData

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class UserReport(
    val users_id: Long,
    val sum_target: Long,
    val sum_hours: Long,
    val diff: Long
)

@Serializable
data class UserReports(
    val userreports: List<UserReport>
)
