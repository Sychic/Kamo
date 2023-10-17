package kamo.commands

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import kamo.modules.Module

object CommandManager : Module() {
    override val name: String = "command"
    private val commands = mutableListOf<Command>()

    override suspend fun setup() {
        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            commands.find { it.name == interaction.invokedCommandName }?.handle(this)
        }
    }

    suspend fun registerCommand(command: Command) {
        commands.add(command)
        kord.createGlobalChatInputCommand(command.name, command.desc, command::setup)
    }
}