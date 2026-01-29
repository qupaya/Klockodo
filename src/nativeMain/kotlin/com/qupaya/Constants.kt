package com.qupaya

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

const val APP_NAME = "Klockodo"

@ExperimentalForeignApi
val userHome = getenv("HOME")?.toKString()
