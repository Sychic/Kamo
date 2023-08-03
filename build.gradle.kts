import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
}

group = "kamo"
version = "0.0.1"

repositories {
    mavenCentral()
    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
    maven {
        name = "Jitpack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    val kordVersion: String by project
    implementation("dev.kord:kord-core:$kordVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("com.google.code.gson:gson:2.9.1")
    val mongoKotlinVersion: String by project
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:$mongoKotlinVersion")
    val hylinVersion: String by project
    implementation("com.github.skytils:hylin:$hylinVersion")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}