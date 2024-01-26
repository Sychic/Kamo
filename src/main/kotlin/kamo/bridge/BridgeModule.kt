package kamo.bridge

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.effectiveName
import dev.kord.core.event.gateway.DisconnectEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.embed
import dev.zerite.craftlib.chat.dsl.chat
import io.ktor.http.*
import kamo.bridge.auth.*
import kamo.bridge.util.*
import kamo.commands.CommandManager
import kamo.modules.Module
import kamo.properties
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext

object BridgeModule : Module(), CoroutineScope {
    override val name: String = "bridge"
    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()

    val colorRegex = Regex("§[0-9a-f]")
    val guildChannel = Snowflake(720038359334125568)
    val officerChannel = Snowflake(721227990528163900)

    /*formatted: §r§[23](Guild|Officer) > §.(\[(?:MVP|VIP)§.\+§.\])? [\w\d_]+ (?:§e\[\w*\])?§f: §r(\w)+*/
    val regex = Regex("(?<channel>Guild|Officer) > \\[?(?<rank>MVP\\+{0,2}|VIP\\+?)?\\]? (?<username>[\\w\\d_]{3,16}) (?:\\[\\w+\\])?: (?<content>.+)")

    var bridge: Bridge? = null
        set(value) {
            field?.let { bridge ->
                bridge.connection?.close(chat { string("Restarting") })
                bridge.cancel()
            }
            field = value
            field?.let { bridge ->
                kord.launch {
                    bridge.setup()
                    kord.getChannelOf<MessageChannel>(guildChannel)?.createMessage { content = "Starting!" }
                }
            }
        }
    val messageFlow = MutableSharedFlow<Message>().also { it.onEach { println(it) } }
    val channels = setOf(720038359334125568UL, 721227990528163900UL)

    override suspend fun setup() {
        kord.on<MessageCreateEvent>(scope = this) {
            if (message.channelId.value !in channels) return@on
            if (message.author?.isBot == true) return@on
            coroutineScope {
                launch {
                    val channel = if (message.channelId == officerChannel) Channel.OFFICER else Channel.GUILD
                    messageFlow.emit(DiscordMessage(message.getAuthorAsMemberOrNull()!!.effectiveName, message.content, channel))
                }
            }
        }
        kord.on<DisconnectEvent.DetachEvent> {
            kord.getChannelOf<MessageChannel>(BridgeModule.guildChannel)?.createMessage { content = "Stopping!" }
        }
        messageFlow.asSharedFlow().filterIsInstance<McMessage>().onEach(::onMessage).launchIn(this + SupervisorJob())
        CommandManager.registerCommand(BridgeCommand)
        setupBridge()
    }

    suspend fun onMessage(mcMessage: McMessage) {
        kord.getChannelOf<MessageChannel>(mcMessage.channel.channel)?.createMessage {
            embed {
                color = when(mcMessage.rank) {
                    "MVP++" -> Color(0xffaa00)
                    "MVP+" -> Color(0x5555ff)
                    "MVP" -> Color(0x55ffff)
                    "VIP+" -> Color(0x00aa00)
                    "VIP" -> Color(0x55ff55)
                    else -> Color(0xaaaaaa)
                }
                val authorStr = "${mcMessage.username}${if(mcMessage.content.length < 256) ": ${mcMessage.content}" else ""}"
                author {
                    name = authorStr
                    icon = "https://crafthead.net/avatar/${UsernameUtil.getUUID(mcMessage.username)}"
                }
                if (mcMessage.content.length >= 256) {
                    description = mcMessage.content
                }
            }
        }
    }

    fun restartBridge() =
        launch {
            bridge = null
            setupBridge()
        }

    suspend fun setupBridge() {
        properties.getProperty("mc")?.let { token ->
            if (requestMcProfile(token).status == HttpStatusCode.Unauthorized) {
                properties.getProperty("refresh")?.let { refreshToken ->
                    println("attempting to refresh token")
                    refreshAccessToken(refreshToken)
                }?.let { authData ->
                    val xblData = obtainXBLToken(authData.access_token)
                    val xstsData = obtainXSTSToken(xblData)
                    val mcTokenData = obtainMCToken(xstsData)
                    bridge = Bridge(mcTokenData.access_token, messageFlow, (BridgeModule + Job()).coroutineContext)
                    println("saving new access token")
                    properties.setProperty("mc", mcTokenData.access_token)
                    properties.store(FileOutputStream("/opt/kamo/config"), null)
                } ?: run { kord.getChannelOf<MessageChannel>(officerChannel)?.createMessage { content = "unable to refresh account, please use /setup again" } }
            } else {
                println("using saved access token")
                bridge = Bridge(token, messageFlow, (BridgeModule + Job()).coroutineContext)
            }
        }
    }
}