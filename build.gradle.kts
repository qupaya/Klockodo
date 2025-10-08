plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.kotlinSerialization)
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
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-linuxx64:1.8.1")
        implementation("org.jetbrains.kotlinx:kotlinx-datetime-linuxx64:0.7.1")
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.serialization)
        implementation(libs.ktor.client.curl)
        implementation(libs.ktor.client.content)
      }
    }
    nativeTest {
      dependencies {
        implementation("org.jetbrains.kotlin:kotlin-test-common")
      }
    }
  }

}
