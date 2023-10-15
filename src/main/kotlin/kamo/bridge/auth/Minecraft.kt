package kamo.bridge.auth

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kamo.Kamo.httpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.*

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
    }.body<MCTokenData>()

suspend fun getMCProfile(access_token: String) =
    httpClient.get {
        url {
            protocol = URLProtocol.HTTPS
            host = "api.minecraftservices.com"
            path("minecraft", "profile")
        }
        bearerAuth(access_token)
    }.body<MCProfileData>()

@Serializable
data class MCTokenData(
    @SerialName("username")
    val uuid: String,
    val access_token: String,
    val token_type: String
)

@Serializable
data class MCProfileData(
    val id: String,
    val name: String
) {
    @Transient
    val uuid = id.chunked(id.length / 2).map { it.toULong(16).toLong() }.let { (first, second) -> UUID(first, second) }
}