package kamo.bridge

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.subCommand
import kamo.Kamo
import kamo.bridge.auth.*
import kamo.commands.Command
import kamo.properties
import kotlinx.coroutines.Job
import kotlinx.coroutines.plus
import java.io.FileOutputStream

object BridgeCommand : Command() {
    override val name: String = "bridge"
    override val desc: String = "Commands related to bridge"

    override suspend fun setup() =
        Kamo.client.createGlobalChatInputCommand(name, desc) {
            defaultMemberPermissions = Permissions(Permission.Administrator)
            subCommand("setup", "sets up bridge")
            subCommand("restart", "restarts bridge")
        }

    override suspend fun handle(event: ChatInputCommandInteractionCreateEvent) {
        val response = event.interaction.deferEphemeralResponse()
        (event.interaction.command as? SubCommand)?.let { subCommand ->
            when(subCommand.name) {
                "setup" -> ::handleSetup
                "restart" -> ::handleRestart
                else -> return
            }(response)
        }
    }

    suspend fun handleSetup(defer: DeferredMessageInteractionResponseBehavior) {
        if (BridgeModule.bridge != null) {
            defer.respond {
                content = "Bridge is already set up!"
            }
            return
        }
        val message = defer.respond { content = "Setting up bridge" }
        runCatching {
            properties.getProperty("refresh")?.let { refreshToken ->
                message.edit { content = "Using refresh token." }
                refreshAccessToken(refreshToken)
            } ?: run {
                val data = authorizeDevice()
                message.edit { content = data.message }
                runCatching {
                    pollUserAuth(data)
                }.onFailure {
                    message.edit {
                        content = "Failed to auth in time."
                    }
                    return
                }.getOrThrow()
            }
        }.onFailure {

        }.onSuccess { authData ->
            message.edit { content = "Saving refresh token." }
            properties.setProperty("refresh", authData.refresh_token)
            properties.store(FileOutputStream("/opt/kamo/config"), null)
            message.edit { content = "Obtaining Xbox Live token." }
            val xblData = obtainXBLToken(authData.access_token)
            val xstsData = obtainXSTSToken(xblData)
            val mcTokenData = obtainMCToken(xstsData)
            message.edit { content = "Creating bridge instance." }
            BridgeModule.bridge = Bridge(mcTokenData.access_token, BridgeModule.messageFlow, (BridgeModule + Job()).coroutineContext)
            message.edit { content = "Saving token." }
            properties.setProperty("mc", mcTokenData.access_token)
            properties.store(FileOutputStream("/opt/kamo/config"), null)
            message.edit { content = "All done!" }
        }
    }

    suspend fun handleRestart(defer: DeferredMessageInteractionResponseBehavior) {
        if (BridgeModule.bridge == null) {
            defer.respond {
                content = "Bridge is not set up yet!"
            }
            return
        }
        val message = defer.respond { content = "Restarting bridge" }
        BridgeModule.restartBridge()
        message.edit { content = "All done!" }
    }
}