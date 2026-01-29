plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
}

group = "com.qupaya.klockodo"
version = "1.0-SNAPSHOT"

kotlin {
    val target = linuxX64("native")

    target.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
        compilations.getByName("main") {
            cinterops {
                val klockodo by creating
            }
        }
    }
    sourceSets {
        nativeMain {
            dependencies {
                implementation(libs.kotlinxCoroutines)
                implementation(libs.kotlinxDatetime)
                implementation(libs.kotlinxIOCore)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.serialization)
                implementation(libs.ktor.client.curl)
                implementation(libs.ktor.client.content)
            }
        }
        nativeTest {
            dependencies {
                implementation(libs.kotestFrameworkEngine)
                implementation(libs.kotestAssertions)
            }
        }
    }
}
