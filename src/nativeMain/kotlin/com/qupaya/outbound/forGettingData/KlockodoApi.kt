package com.qupaya.outbound.forGettingData

import com.qupaya.APP_NAME
import com.qupaya.klockodo.outboundPorts.ApiError
import com.qupaya.klockodo.outboundPorts.ApiResult
import com.qupaya.klockodo.outboundPorts.ForBuildingEntryRequests
import com.qupaya.klockodo.outboundPorts.ForGettingData
import com.qupaya.klockodo.outboundPorts.map
import com.qupaya.outbound.error.Error
import com.qupaya.outbound.error.Errors
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.curl.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class KlockodoApi(private val apiKey: String, private val apiUser: String) : ForGettingData {
    private val client = HttpClient(Curl) {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            header("X-ClockodoApiUser", apiUser)
            header("X-ClockodoApiKey", apiKey)
            header("X-Clockodo-External-Application", "$APP_NAME;andreas@qupaya.com")
        }
    }

    private val errorJson = Json

    private suspend inline fun <reified T> HttpResponse.decode(): ApiResult<T> =
        try {
            ApiResult.Success(body<T>())
        } catch (_: JsonConvertException) {
            ApiResult.Failure(readErrors())
        }

    private suspend fun HttpResponse.readErrors(): List<ApiError> {
        val text = bodyAsText()
        return try {
            errorJson.decodeFromString<Errors>(text).errors
        } catch (_: SerializationException) {
            try {
                listOf(errorJson.decodeFromString<Error>(text))
            } catch (_: SerializationException) {
                listOf(Error("Unexpected response (${status}): $text"))
            }
        }
    }

    override fun fetchOpenWorkTimeOfYear(year: Int): ApiResult<Duration> = runBlocking {
        try {
            client.get("https://my.clockodo.com/api/userreports?year=${year}")
                .decode<UserReports>()
                .map { (-it.userreports.first().diff).toDuration(DurationUnit.SECONDS) }
        } catch (e: IllegalStateException) {
            // Internet unavailable
            throw e
        }
    }

    override fun fetchWorkedTimeOfDay(day: LocalDate): ApiResult<Duration> = runBlocking {
        try {
            val tomorrow = day.plus(1, DateTimeUnit.DAY)
            client.get("https://my.clockodo.com/api/v2/entries?time_since=${day}T00:00:00Z&time_until=${tomorrow}T00:00:00Z")
                .decode<Entries>()
                .map { it.totalDuration }
        } catch (e: IllegalStateException) {
            // Internet unavailable
            throw e
        }
    }

    override fun getCurrentTimeEntry(): ApiResult<ForGettingData.TimeEntry?> = runBlocking {
        try {
            client.get("https://my.clockodo.com/api/v2/clock")
                .decode<RunningTimeEntry>()
                .map { it.running }
        } catch (e: IllegalStateException) {
            // Internet unavailable
            throw e
        }
    }

    override fun startTimeEntry(request: ForBuildingEntryRequests.EntryRequest): ApiResult<ForGettingData.StartedStoppedEntries> =
        runBlocking {
            if (request !is EntryRequest) {
                throw IllegalArgumentException("Entry request must be of type EntryRequest")
            }

            try {
                client.post("https://my.clockodo.com/api/v2/clock") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                    .decode<RunningTimeEntry>()
                    .map { entry ->
                        object : ForGettingData.StartedStoppedEntries {
                            override val startedEntry = entry.running
                            override val stoppedEntry = entry.stopped
                        }
                    }
            } catch (e: IllegalStateException) {
                // Internet unavailable
                throw e
            }
        }

    override fun stopTimeEntry(entry: ForGettingData.TimeEntry): ApiResult<ForGettingData.TimeEntry?> = runBlocking {
        if (entry !is TimeEntry) {
            throw IllegalArgumentException("Entry must be of type TimeEntry")
        }
        try {
            client.delete("https://my.clockodo.com/api/v2/clock/${entry.id}")
                .decode<RunningTimeEntry>()
                .map { it.stopped }
        } catch (e: IllegalStateException) {
            // Internet unavailable
            throw e
        }
    }
}
