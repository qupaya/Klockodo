package com.qupaya

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlin.time.Duration

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class Configuration(
    val apiKey: String,
    val apiUser: String,
    val defaultProject: Int,
    val workTimePerDay: Duration
) {
    companion object {
        @OptIn(ExperimentalForeignApi::class)
        fun load(): Configuration {
            if (userHome == null) {
                throw RuntimeException("No home directory found")
            }
            Duration.serializer()

            val userConfigPath = Path(userHome, ".config", APP_NAME, "config.json")
            if (!SystemFileSystem.exists(userConfigPath)) {
                throw RuntimeException("Configuration file not found: $userConfigPath")
            }

            SystemFileSystem.source(userConfigPath).buffered().use {
                return Json.decodeFromString<Configuration>(it.readString())
            }
        }
    }
}