plugins {
    kotlin("jvm") version "2.3.0"
    id("com.google.devtools.ksp") version "2.3.0"
    id("fr.smolder.hytale.dev") version "0.1.0"
}

group = "scot.oskar"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.hytale.com/release")
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly("com.hypixel.hytale:Server:2026.02.19-1a311a592")

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)

    implementation(libs.hytalecodec.runtime)
    implementation(libs.hytalecodec.annotation)
    ksp(libs.hytalecodec.processor)

    implementation(libs.coroutines)

    implementation("org.postgresql:postgresql:42.7.9")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

hytale {
    manifest {
        group = "scot.oskar"
        name = "GalaxyFactions"
        version = project.version.toString()
        description = "The Ultimate Factions Plugin"
        author("oskarscot")

        serverVersion = "2026.02.19-1a311a592"

        main = "scot.oskar.galaxyfactions.FactionsPlugin"

        includesAssetPack = false
    }

    hytalePath.set("/home/oskar/.var/app/com.hypixel.HytaleLauncher/data/Hytale")

    minMemory.set("2G")
    maxMemory.set("4G")

    vineflowerVersion.set("1.11.2")
    decompileFilter.set(listOf("com/hypixel/**"))
    decompilerHeapSize.set("6G")
}