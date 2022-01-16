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
    val kordexVersion: String by project
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:$kordexVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    val kmongoVersion: String by project
    implementation("org.litote.kmongo:kmongo-coroutine:$kmongoVersion")
    val hylinVersion: String by project
    implementation("com.github.skytils:hylin:$hylinVersion")
    implementation("org.apache.httpcomponents:httpclient:4.3.3")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}