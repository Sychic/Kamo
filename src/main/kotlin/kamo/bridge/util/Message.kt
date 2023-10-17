package kamo.bridge.util

open class Message
data class McMessage(val content: String) : Message()

data class DiscordMessage(val author: String, val content: String, val channel: Channel = Channel.GUILD) : Message()

enum class Channel(val command: String) {
    GUILD("/gc"),
    OFFICER("/oc")
}