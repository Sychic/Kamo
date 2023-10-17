package kamo.commands

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder

abstract class Command {
    abstract val name: String
    abstract val desc: String

    context(GlobalChatInputCreateBuilder)
    abstract fun setup()

    abstract suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent)
}