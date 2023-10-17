package kamo.bridge.util

open class Message
data class McMessage(val content: String) : Message()

data class DiscordMessage(val author: String, val content: String) : Message()