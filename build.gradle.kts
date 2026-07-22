import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.compose") version "1.6.1"
}

group = "com.smartagents"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

compose.desktop {
    application {
        mainClass = "com.smartagents.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "SmartAgents-ProjectManager"
            packageVersion = "1.0.0"

            windows {
                menuGroup = "SmartAgents"
                upgradeUuid = "a8b9c0d1-e2f3-4567-89ab-cdef01234567"
            }
        }
    }
}
