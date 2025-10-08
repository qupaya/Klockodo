@file:OptIn(ExperimentalForeignApi::class)

package com.qupaya.klockodo

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.posix.getenv

@ExperimentalForeignApi
val userHome = getenv("HOME")?.toKString()

const val APP_NAME = "Klockodo"

@Serializable
data class Configuration(val apiKey: String, val apiUser: String, val defaultProject: Int, val workSecondsPerDay: Long) {
  companion object {
    const val CONFIGURATION_FILE = "/.config/Klockodo/config.json"
    val userConfigFile = "$userHome$CONFIGURATION_FILE"

    fun load(): Configuration {
      if (!PosixFiles.exists(userConfigFile)) {
        throw RuntimeException("Configuration file not found: $userConfigFile")
      }

      var configContent = ""
      PosixFiles.readLines(userConfigFile) { s -> configContent += s }
      return Json.decodeFromString<Configuration>(configContent)
    }
  }
}
