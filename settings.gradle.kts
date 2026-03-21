rootProject.name = "GalaxyFactions"

pluginManagement {
    repositories {
        maven("https://repo.smolder.fr/public/")
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}