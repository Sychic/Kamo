package kamo.bridge.util

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kamo.Kamo
import kamo.Kamo.httpClient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

object UsernameUtil {
    val cache = mutableMapOf<String, String>()

    suspend fun getUsername(uuid: String) =
        httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.ashcon.app"
                path("mojang", "v2", "user", uuid)
            }

        }.body<JsonObject>()["username"]!!.jsonPrimitive.content

    suspend fun getUUID(username: String) =
        cache.getOrPut(username) {
            httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.ashcon.app"
                    path("mojang", "v2", "user", username)
                }

            }.body<JsonObject>()["uuid"]!!.jsonPrimitive.content
        }
}