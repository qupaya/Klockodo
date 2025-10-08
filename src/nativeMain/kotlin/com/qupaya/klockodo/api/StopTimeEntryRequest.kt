package com.qupaya.klockodo.api

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
data class StopTimeEntryRequest(
  @Contextual
  @Serializable(with = InstantSerializer::class)
  val time_until: Instant = Clock.System.now()
)
