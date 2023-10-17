package kamo.bridge.util

import dev.kord.common.entity.Snowflake
import kamo.bridge.BridgeModule

open class Message
data class McMessage(val channel: Channel, val rank: String?, val username: String, val content: String) : Message()

data class DiscordMessage(val author: String, val content: String, val channel: Channel = Channel.GUILD) : Message()

enum class Channel(val command: String, val channel: Snowflake) {
    GUILD("/gc", BridgeModule.guildChannel),
    OFFICER("/oc", BridgeModule.officerChannel)
}