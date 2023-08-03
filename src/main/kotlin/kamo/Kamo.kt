package kamo

import com.mongodb.ConnectionString
import com.mongodb.kotlin.client.coroutine.MongoClient
import dev.kord.cache.api.count
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.cache.data.GuildData
import dev.kord.core.cache.data.UserData
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kamo.modules.Module
import kamo.modules.impl.JoinModule
import kamo.modules.impl.VerifyModule
import skytils.hylin.HylinAPI
import skytils.hylin.HylinAPI.Companion.createHylinAPI
import java.util.*

suspend fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)
    Kamo.init(System.getProperty("kamo.token") ?:
        run {
            println("Please input token")
            scanner.nextLine()
        },
        System.getProperty("kamo.mongo") ?:
        run {
            println("Please input mongo link")
            scanner.nextLine()
        },
        System.getProperty("kamo.api") ?:
        run {
            println("Please input hypixel api key")
            scanner.nextLine()
        }
    )
}

object Kamo {
    lateinit var client: Kord
    lateinit var mongo: MongoClient
    lateinit var hylin: HylinAPI
    private val modules: MutableList<Module> = mutableListOf()

    @OptIn(PrivilegedIntent::class)
    suspend fun init(discordToken: String, mongoURL: String, apiKey: String) {
        mongo = MongoClient.create(ConnectionString(mongoURL))
        client = Kord(discordToken)
        hylin = client.createHylinAPI(apiKey)

        modules.add(VerifyModule)
        modules.add(JoinModule)

        modules.forEach {
            it.setup()
        }

        client.login {
            presence {
                val users = client.cache.count<UserData>()
                val guilds = client.cache.count<GuildData>()
                status = PresenceStatus.Online
                watching("$users users and $guilds guilds")
            }
            intents = Intents.all
        }


    }
}