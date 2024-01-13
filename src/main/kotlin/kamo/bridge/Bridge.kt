package kamo.bridge

import com.google.gson.JsonObject
import dev.zerite.craftlib.protocol.Packet
import dev.zerite.craftlib.protocol.connection.NettyConnection
import dev.zerite.craftlib.protocol.connection.PacketHandler
import dev.zerite.craftlib.protocol.connection.PacketSendingEvent
import dev.zerite.craftlib.protocol.packet.handshake.client.ClientHandshakePacket
import dev.zerite.craftlib.protocol.packet.login.client.ClientLoginEncryptionResponsePacket
import dev.zerite.craftlib.protocol.packet.login.client.ClientLoginStartPacket
import dev.zerite.craftlib.protocol.packet.login.server.ServerLoginEncryptionRequestPacket
import dev.zerite.craftlib.protocol.packet.login.server.ServerLoginSetCompressionPacket
import dev.zerite.craftlib.protocol.packet.login.server.ServerLoginSuccessPacket
import dev.zerite.craftlib.protocol.packet.play.client.display.ClientPlayChatMessagePacket
import dev.zerite.craftlib.protocol.packet.play.client.other.ClientPlayKeepAlivePacket
import dev.zerite.craftlib.protocol.packet.play.server.display.ServerPlayChatMessagePacket
import dev.zerite.craftlib.protocol.packet.play.server.join.ServerPlayJoinGamePacket
import dev.zerite.craftlib.protocol.packet.play.server.other.ServerPlayKeepAlivePacket
import dev.zerite.craftlib.protocol.packet.play.server.other.ServerPlaySetCompressionPacket
import dev.zerite.craftlib.protocol.util.Crypto
import dev.zerite.craftlib.protocol.version.MinecraftProtocol
import dev.zerite.craftlib.protocol.version.ProtocolVersion
import io.ktor.client.request.*
import io.ktor.http.*
import io.netty.handler.timeout.ReadTimeoutException
import kamo.Kamo
import kamo.bridge.BridgeModule.guildChannel
import kamo.bridge.BridgeModule.officerChannel
import kamo.bridge.BridgeModule.restartBridge
import kamo.bridge.auth.*
import kamo.bridge.util.Channel
import kamo.bridge.util.DiscordMessage
import kamo.bridge.util.McMessage
import kamo.bridge.util.Message
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.math.BigInteger
import java.net.InetAddress
import java.security.MessageDigest
import java.security.PublicKey
import javax.crypto.SecretKey
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates
import kotlin.text.toByteArray

class Bridge(val token: String, val messageFlow: MutableSharedFlow<Message>,
             override val coroutineContext: CoroutineContext): PacketHandler, CoroutineScope {
    var profile: MCProfileData by Delegates.notNull()
    var connection: NettyConnection? = null

    suspend fun setup() {
        coroutineScope {
            profile = getMCProfile(token)
            launch {
                connection = MinecraftProtocol.connect(withContext(Dispatchers.IO) {
                    InetAddress.getByName("play.hypixel.net")
                }, 25565) {
                    debug = false
                    handler = this@Bridge
                }
            }
            println("set up bridge")
        }
    }

    override fun connected(connection: NettyConnection) {
        connection.version = ProtocolVersion.MC1_8
        connection.state = MinecraftProtocol.HANDSHAKE
        connection.send(
            ClientHandshakePacket(
                ProtocolVersion.MC1_8,
                "hypixel.net",
                25565,
                MinecraftProtocol.LOGIN
            )
        ) {
            connection.state = MinecraftProtocol.LOGIN
            connection.send(ClientLoginStartPacket(profile.name)) {
                launch {
                    messageFlow.filterIsInstance<DiscordMessage>().onEach { message ->
                        println(message)
                        connection.send(ClientPlayChatMessagePacket("${message.channel.command} ${message.author} > ${message.content.replace("ez", "easy", true)}".take(256)))
                    }.launchIn(this)
                }
            }
        }
    }

    override fun received(connection: NettyConnection, packet: Packet) {
        when(packet) {
            is ServerLoginEncryptionRequestPacket -> {
                val secretKey = Crypto.newSecretKey()
                launch {
                    val serverId = getServerId(packet.serverId, packet.publicKey, secretKey)
                    Kamo.httpClient.post {
                        url {
                            protocol = URLProtocol.HTTPS
                            host = "sessionserver.mojang.com"
                            path("session", "minecraft", "join")
                        }
                        contentType(ContentType.Application.Json)
                        setBody(
                            JsonObject().apply {
                                addProperty("accessToken", token)
                                addProperty("selectedProfile", profile.uuid.toString())
                                addProperty("serverId", serverId)
                            }.toString()
                        )
                    }
                }.invokeOnCompletion {
                    connection.send(
                        ClientLoginEncryptionResponsePacket(
                            packet.publicKey,
                            secretKey,
                            packet.verifyToken
                        )
                    ) {
                        connection.enableEncryption(secretKey)
                    }
                }
            }
            is ServerLoginSetCompressionPacket -> connection.compressionThreshold = packet.threshold
            is ServerLoginSuccessPacket -> {
                println("connected")
                connection.state = MinecraftProtocol.PLAY
            }
            is ServerPlaySetCompressionPacket -> connection.compressionThreshold = packet.threshold
            is ServerPlayKeepAlivePacket -> connection.send(ClientPlayKeepAlivePacket(packet.id))
            is ServerPlayJoinGamePacket -> {
                println("Sending to limbo")
                connection.send(ClientPlayChatMessagePacket("§"))
            }
            is ServerPlayChatMessagePacket -> {
                if (packet.message.unformattedText.contains(profile.name)) return
                println(packet.message.unformattedText)
                launch {
                    BridgeModule.regex.find(packet.message.unformattedText.replace(BridgeModule.colorRegex, ""))?.groups?.let { groups ->
                        messageFlow.emit(
                            McMessage(
                                if (groups["channel"]?.value == "Officer") Channel.OFFICER else Channel.GUILD,
                                groups["rank"]?.value,
                                groups["username"]!!.value,
                                groups["content"]!!.value
                            )
                        )
                    }
                }
            }
        }
    }

    override fun sending(connection: NettyConnection, event: PacketSendingEvent) {
        if (event.packet is ClientPlayChatMessagePacket) {
            println((event.packet as ClientPlayChatMessagePacket).message)
        }
    }

    override fun exception(connection: NettyConnection, cause: Throwable) {
        if (cause is ReadTimeoutException) {
            restartBridge()
        }
    }

    private fun getServerId(base: String, publicKey: PublicKey, secretKey: SecretKey): String {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(base.toByteArray(Charsets.ISO_8859_1))
        digest.update(secretKey.encoded)
        digest.update(publicKey.encoded)
        return BigInteger(digest.digest()).toString(16)
    }
}