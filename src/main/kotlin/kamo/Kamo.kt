package kamo

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.mongodb.ConnectionString
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kamo.modules.JoinModule
import kamo.modules.PresenceModule
import kamo.modules.VerifyModule
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
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
    lateinit var client: ExtensibleBot
    lateinit var mongo: CoroutineClient
    lateinit var hylin: HylinAPI

    @OptIn(PrivilegedIntent::class)
    suspend fun init(discordToken: String, mongoURL: String, apiKey: String) {
        mongo = KMongo.createClient(ConnectionString(mongoURL)).coroutine
        client = ExtensibleBot(discordToken) {
            chatCommands {
                enabled = true
            }
            
            intents {
                +Intents.all
            }

            presence {
                status = PresenceStatus.Idle
                competing("hi")
            }

            extensions {
                add { JoinModule }
                add { PresenceModule }
                add { VerifyModule }
            }

        }
        hylin = client.getKoin().get<Kord>().createHylinAPI(apiKey)

        client.start()


    }
}