package com.qupaya.outbound.error

import com.qupaya.klockodo.outboundPorts.ApiError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class Error(
    override val message: String,
    override val fields: List<String>? = null,
    override val path: String? = null,
) : ApiError

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class Errors(val errors: List<Error>)