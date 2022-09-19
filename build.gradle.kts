import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
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
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.18.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("com.google.code.gson:gson:2.9.1")
    val kmongoVersion: String by project
    implementation("org.litote.kmongo:kmongo-coroutine:$kmongoVersion")
    val hylinVersion: String by project
    implementation("com.github.skytils:hylin:$hylinVersion")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.1.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}