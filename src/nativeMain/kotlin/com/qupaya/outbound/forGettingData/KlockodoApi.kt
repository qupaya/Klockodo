package com.qupaya.outbound.forGettingData

import com.qupaya.APP_NAME
import com.qupaya.klockodo.outboundPorts.ForBuildingEntryRequests
import com.qupaya.klockodo.outboundPorts.ForGettingData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.curl.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
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

    override fun fetchOpenWorkTimeOfYear(year: Int): Duration = runBlocking {
        try {
            val response = client.get("https://my.clockodo.com/api/userreports?year=${year}")

            val report = response.body<UserReports>().userreports.first()
            return@runBlocking (-report.diff).toDuration(DurationUnit.SECONDS)
        } catch (e: IllegalStateException) {
            // Internet unavailable
            throw e
        }
    }

    override fun fetchWorkedTimeOfDay(day: LocalDate): Duration = runBlocking {
        try {
            val tomorrow = day.plus(1, DateTimeUnit.DAY)
            val response =
                client.get("https://my.clockodo.com/api/v2/entries?time_since=${day}T00:00:00Z&time_until=${tomorrow}T00:00:00Z")

            val entries = response.body<Entries>()
            return@runBlocking entries.totalDuration
        } catch (e: IllegalStateException) {
            // Internet unavailable
            throw e
        }
    }

    override fun getCurrentTimeEntry(): ForGettingData.TimeEntry? = runBlocking {
        try {
            val response = client.get("https://my.clockodo.com/api/v2/clock")

            return@runBlocking response.body<RunningTimeEntry>().running
        } catch (e: IllegalStateException) {
            // Internet unavailable
            throw e
        } catch (_: Exception) {
            return@runBlocking null
        }
    }

    override fun startTimeEntry(request: ForBuildingEntryRequests.EntryRequest): ForGettingData.StartedStoppedEntries =
        runBlocking {
            if (request !is EntryRequest) {
                throw IllegalArgumentException("Entry request must be of type EntryRequest")
            }

            try {
                val response = client.post("https://my.clockodo.com/api/v2/clock") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                val entry = response.body<RunningTimeEntry>()

                return@runBlocking object : ForGettingData.StartedStoppedEntries {
                    override val startedEntry = entry.running
                    override val stoppedEntry = entry.stopped
                }
            } catch (e: IllegalStateException) {
                // Internet unavailable
                throw e
            }
        }

    override fun stopTimeEntry(entry: ForGettingData.TimeEntry): ForGettingData.TimeEntry? = runBlocking {
        if (entry !is TimeEntry) {
            throw IllegalArgumentException("Entry must be of type TimeEntry")
        }
        try {
            val response = client.delete("https://my.clockodo.com/api/v2/clock/${entry.id}")

            return@runBlocking response.body<RunningTimeEntry>().stopped
        } catch (e: IllegalStateException) {
            // Internet unavailable
            throw e
        }
    }
}