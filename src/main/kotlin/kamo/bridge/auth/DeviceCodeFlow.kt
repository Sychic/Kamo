package kamo.bridge.auth

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kamo.bridge.auth.Auth.CLIENT_ID
import kamo.bridge.auth.Auth.SCOPE
import kamo.bridge.auth.Auth.TENANT
import kamo.bridge.auth.Auth.httpClient
import kamo.bridge.auth.Auth.json
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.lang.IllegalStateException
import java.time.Duration
import java.time.Instant

suspend fun authorizeDevice() =
    httpClient.post {
        url {
            protocol = URLProtocol.HTTPS
            host = "login.microsoftonline.com"
            path(TENANT, "oauth2", "v2.0", "devicecode")
        }
        contentType(ContentType.Application.FormUrlEncoded)

        setBody(
            FormDataContent(
                parameters {
                    append("client_id", CLIENT_ID)
                    append("scope", SCOPE)
                }
            )
        )
    }.body<DeviceCodeData>()

suspend fun pollUserAuth(data: DeviceCodeData): UserAuthData {
    val end = Instant.now() + Duration.ofSeconds(data.expires_in.toLong())
    var delay = data.interval
    while (Instant.now() < end) {
        val authData = checkUserAuth(data)
        when (authData["error"].toString()) {
            "authorization_pending" -> {
                delay(delay * 1000L)
                continue
            }
            "slow_down" -> {
                delay += 5
                delay(delay * 1000L)
                continue
            }
        }
        if (authData.containsKey("access_token")) {
            return json.decodeFromJsonElement<UserAuthData>(authData)
        }
    }
    throw IllegalStateException("User failed auth")
}

suspend fun checkUserAuth(data: DeviceCodeData) =
    httpClient.post {
        url {
            protocol = URLProtocol.HTTPS
            host = "login.microsoftonline.com"
            path(TENANT, "oauth2", "v2.0", "token")
        }
        contentType(ContentType.Application.FormUrlEncoded)

        setBody(
            FormDataContent(
                parameters {
                    append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    append("client_id", CLIENT_ID)
                    append("device_code", data.device_code)
                }
            )
        )
    }.body<JsonObject>()

suspend fun refreshAccessToken(data: UserAuthData) =
    httpClient.post {
        url {
            protocol = URLProtocol.HTTPS
            host = "login.microsoftonline.com"
            path(TENANT, "oauth2", "v2.0", "token")
        }
        contentType(ContentType.Application.FormUrlEncoded)

        setBody(
            FormDataContent(
                parameters {
                    append("cliend_id", CLIENT_ID)
                    append("grant_type", "refresh_token")
                    append("refresh_token", data.refresh_token)
                    append("scope", SCOPE)
                }
            )
        )
    }.body<UserAuthData>()

@Serializable
data class DeviceCodeData(
    val user_code: String,
    val device_code: String,
    val verification_uri: String,
    val expires_in: Int,
    val interval: Int,
    val message: String
)

@Serializable
data class UserAuthData(
    val token_type: String,
    val scope: String,
    val expires_in: Int,
    val access_token: String,
    val refresh_token: String
)