package kamo.commands

import dev.kord.core.entity.application.ChatInputCommandCommand
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent

abstract class Command {
    abstract val name: String
    abstract val desc: String

    abstract suspend fun setup(): ChatInputCommandCommand

    abstract suspend fun handle(event: ChatInputCommandInteractionCreateEvent)
}