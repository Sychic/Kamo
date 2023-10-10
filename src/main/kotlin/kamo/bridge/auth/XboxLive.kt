package kamo.bridge.auth

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kamo.Kamo.httpClient
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*

suspend fun obtainXBLToken(accessToken: String) =
    httpClient.post {
        url {
            protocol = URLProtocol.HTTPS
            host = "user.auth.xboxlive.com"
            path("user", "authenticate")
        }
        contentType(ContentType.Application.Json)
        setBody(
            JsonObject(mapOf(
                "Properties" to JsonObject(mapOf(
                    "AuthMethod" to JsonPrimitive("RPS"),
                    "SiteName" to JsonPrimitive("user.auth.xboxlive.com"),
                    "RpsTicket" to JsonPrimitive("d=$accessToken")
                )),
                "RelyingParty" to JsonPrimitive("http://auth.xboxlive.com"),
                "TokenType" to JsonPrimitive("JWT")
            ))
        )
    }.body<JsonObject>().xblToken()

private fun JsonObject.xblToken(): XBLToken {
    val token = get("Token")?.jsonPrimitive?.content ?: throw IllegalStateException("Missing XBL token")
    val userhash = get("DisplayClaims")?.jsonObject?.
        get("xui")?.jsonArray?.
        get(0)?.jsonObject?.get("uhs")?.
        jsonPrimitive?.content ?: throw IllegalStateException("Malformed xbl token data: missing user hash")
    val expiry = get("NotAfter")?.jsonPrimitive?.content ?: throw IllegalStateException("Malformed xbl token data: missing expiration")
    return XBLToken(token, userhash, Instant.parse(expiry))
}

data class XBLToken(
    val token: String,
    val userHash: String,
    val expiry: Instant
)

suspend fun obtainXSTSToken(xblToken: XBLToken) =
    httpClient.post {
        url {
            protocol = URLProtocol.HTTPS
            host = "xsts.auth.xboxlive.com"
            path("xsts", "authorize")
        }
        contentType(ContentType.Application.Json)
        setBody(
            JsonObject(mapOf(
                "Properties" to JsonObject(mapOf(
                    "SandboxId" to JsonPrimitive("RETAIL"),
                    "UserTokens" to JsonArray(listOf(JsonPrimitive(xblToken.token)))
                )),
                "RelyingParty" to JsonPrimitive("rp://api.minecraftservices.com/"),
                "TokenType" to JsonPrimitive("JWT")
            ))
        )
    }.body<JsonObject>().xstsToken()

private fun JsonObject.xstsToken(): XSTSToken {
    val token = get("Token")?.jsonPrimitive?.content ?: throw IllegalStateException("Missing xsts token")
    val userhash = get("DisplayClaims")?.jsonObject?.
    get("xui")?.jsonArray?.
    get(0)?.jsonObject?.get("uhs")?.
    jsonPrimitive?.content ?: throw IllegalStateException("Malformed xsts token data: missing user hash")
    val expiry = get("NotAfter")?.jsonPrimitive?.content ?: throw IllegalStateException("Malformed xsts token data: missing expiration")
    return XSTSToken(token, userhash, Instant.parse(expiry))
}

data class XSTSToken(
    val token: String,
    val userHash: String,
    val expiry: Instant
)
