package kamo.bridge.auth

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object Auth {
    const val TENANT = "consumers"
    const val CLIENT_ID = "5f9ac917-1d6b-48eb-9b42-70916b21ef6a"
    const val SCOPE = "XboxLive.signin offline_access"
    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }
}