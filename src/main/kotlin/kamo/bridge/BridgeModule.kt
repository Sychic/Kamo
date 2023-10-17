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
import io.ktor.http.*
import kamo.Kamo
import kamo.bridge.auth.*
import kamo.bridge.util.DiscordMessage
import kamo.bridge.util.McMessage
import kamo.bridge.util.Message
import kamo.bridge.util.UsernameUtil
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
    val regex = Regex("(?<channel>Guild|Officer) > \\[?(?<rank>MVP\\+?|VIP\\+?)?\\]? (?<username>[\\w\\d_]{3,16}) (?:\\[\\w+\\])?: (?<content>.+)")

    var bridge: Bridge? = null
        set(value) {
            field = value
            field?.let { bridge ->
                kord.launch {
                    bridge.setup()
                    kord.getChannelOf<MessageChannel>(guildChannel)?.createMessage { content = "Starting!" }
                    messageFlow.asSharedFlow().filterIsInstance<McMessage>().onEach(::onMessage).launchIn(this + SupervisorJob())
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
                    messageFlow.emit(DiscordMessage(message.getAuthorAsMemberOrNull()!!.effectiveName, message.content))
                }
            }
        }
        kord.on<DisconnectEvent.DetachEvent> {
            kord.getChannelOf<MessageChannel>(BridgeModule.guildChannel)?.createMessage { content = "Stopping!" }
        }
        CommandManager.registerCommand(BridgeCommand)
        properties.getProperty("mc")?.let { token ->
            if (requestMcProfile(token).status == HttpStatusCode.Unauthorized) {
                properties.getProperty("refresh")?.let { refreshToken ->
                    println("attempting to refresh token")
                    refreshAccessToken(refreshToken)
                }?.let { authData ->
                    val xblData = obtainXBLToken(authData.access_token)
                    val xstsData = obtainXSTSToken(xblData)
                    val mcTokenData = obtainMCToken(xstsData)
                    bridge = Bridge(mcTokenData.access_token, messageFlow)
                    println("saving new access token")
                    properties.setProperty("mc", mcTokenData.access_token)
                    properties.store(FileOutputStream("/opt/kamo/config"), null)
                } ?: run { kord.getChannelOf<MessageChannel>(officerChannel)?.createMessage { content = "unable to refresh account, please use /setup again" } }
            } else {
                println("using saved access token")
                bridge = Bridge(token, messageFlow)
            }
        }
    }

    suspend fun onMessage(mcMessage: McMessage) {
        val unformatted = mcMessage.content.replace(colorRegex, "")
        regex.find(unformatted)?.groups?.let { groups ->
            val channel = if (groups["channel"]?.value == "Officer") officerChannel else guildChannel
            kord.getChannelOf<MessageChannel>(channel)?.createMessage {
                embed {
                    color = when(groups["rank"]?.value) {
                        "MVP++" -> Color(0xffaa00)
                        "MVP+" -> Color(0x5555ff)
                        "MVP" -> Color(0x55ffff)
                        "VIP+" -> Color(0x00aa00)
                        "VIP" -> Color(0x55ff55)
                        else -> Color(0xaaaaaa)
                    }
                    author {
                        name = "${groups["username"]!!.value}: ${groups["content"]?.value}"
                        icon = "https://crafthead.net/avatar/${UsernameUtil.getUUID(groups["username"]!!.value)}"
                    }
                }
            }
        }
    }
}