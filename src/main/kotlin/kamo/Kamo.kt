package kamo

import com.mongodb.ConnectionString
import com.mongodb.kotlin.client.coroutine.MongoClient
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kamo.bridge.BridgeModule
import kamo.commands.CommandManager
import kamo.modules.Module
import kamo.modules.impl.JoinModule
import kamo.modules.impl.VerifyModule
import kotlinx.serialization.json.Json
import skytils.hylin.HylinAPI
import skytils.hylin.HylinAPI.Companion.createHylinAPI

object Kamo {
    lateinit var client: Kord
    lateinit var mongo: MongoClient
    lateinit var hylin: HylinAPI
    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }
    private val modules: MutableList<Module> = mutableListOf()

    @OptIn(PrivilegedIntent::class)
    suspend fun init(discordToken: String, mongoURL: String, apiKey: String) {
        mongo = MongoClient.create(ConnectionString(mongoURL))
        client = Kord(discordToken)
        hylin = client.createHylinAPI(apiKey)

        modules.add(BridgeModule)
        modules.add(CommandManager)
        modules.add(JoinModule)
        modules.add(VerifyModule)

        modules.forEach {
            it.setup()
        }

        client.on<ReadyEvent> {
            client.editPresence {
                watching("${guilds.size} guilds")
            }
        }

        client.login {
            presence {
                status = PresenceStatus.Online
            }
            intents = Intents.all
        }


    }
}