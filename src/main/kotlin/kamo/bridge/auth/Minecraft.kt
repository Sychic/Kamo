package kamo.bridge.auth

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kamo.Kamo.httpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

suspend fun obtainMCToken(xstsToken: XSTSToken) =
    httpClient.post {
        url {
            protocol = URLProtocol.HTTPS
            host = "api.minecraftservices.com"
            path("authentication", "login_with_xbox")
        }
        contentType(ContentType.Application.Json)
        setBody(
            JsonObject(mapOf(
                "identityToken" to JsonPrimitive("XBL3.0 x=${xstsToken.userHash};${xstsToken.token}")
            ))
        )
    }.body<JsonObject>()

@Serializable
data class MCTokenData(
    @SerialName("username")
    val uuid: String,
    val access_token: String,
    val token_type: String
)