package com.qupaya.klockodo.api

import com.qupaya.klockodo.APP_NAME
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
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.plus
import kotlin.time.ExperimentalTime

class KlockodoApi(private val apiKey: String, private val apiUser: String) {
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

  @Throws(IllegalStateException::class)
  fun getUserReport(today: LocalDate): UserReport = runBlocking {
    try {
      val response = client.get("https://my.clockodo.com/api/userreports?year=${today.year}")

      return@runBlocking response.body<UserReports>().userreports.first()
    } catch (e: IllegalStateException) {
      // Internet unavailable
      throw e
    }
  }

  @OptIn(ExperimentalTime::class)
  @Throws(IllegalStateException::class)
  fun getEntriesOfDay(day: LocalDate): Entries = runBlocking {
    try {
      val tomorrow = day.plus(1, DateTimeUnit.DAY)
      val response = client.get("https://my.clockodo.com/api/v2/entries?time_since=${day}T00:00:00Z&time_until=${tomorrow}T00:00:00Z")

      return@runBlocking response.body<Entries>()
    } catch (e: IllegalStateException) {
      // Internet unavailable
      throw e
    }
  }

  @Throws(IllegalStateException::class)
  fun loadRunningTimeEntry(): TimeEntry? = runBlocking {
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

  @Throws(IllegalStateException::class)
  @OptIn(ExperimentalTime::class, FormatStringsInDatetimeFormats::class)
  fun logTimeEntry(entry: EntryRequest): RunningTimeEntry = runBlocking {
    try {
      val response = client.post("https://my.clockodo.com/api/v2/clock") {
        contentType(ContentType.Application.Json)
        setBody(entry)
      }

      return@runBlocking response.body()
    } catch (e: IllegalStateException) {
      // Internet unavailable
      throw e
    }
  }

  @OptIn(ExperimentalTime::class)
  @Throws(IllegalStateException::class)
  fun stopTimeEntry(timeEntry: TimeEntry): RunningTimeEntry = runBlocking {
    try {
      val response = client.delete("https://my.clockodo.com/api/v2/clock/${timeEntry.id}")
      println(response)

      return@runBlocking response.body()
    } catch (e: IllegalStateException) {
      // Internet unavailable
      throw e
    }
  }
}