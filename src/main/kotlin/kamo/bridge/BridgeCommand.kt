package kamo.bridge

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import kamo.bridge.auth.*
import kamo.commands.Command
import kamo.properties
import java.io.FileOutputStream

object BridgeCommand : Command() {
    override val name: String = "setup"
    override val desc: String = "sets up bridge"

    context(GlobalChatInputCreateBuilder)
    override fun setup() {
        defaultMemberPermissions = Permissions(Permission.Administrator)
    }

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        if (BridgeModule.bridge != null) {
            event.interaction.respondEphemeral {
                content = "Bridge is already set up!"
            }
            return
        }
        val defer = event.interaction.deferEphemeralResponse()
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
            BridgeModule.bridge = Bridge(mcTokenData.access_token, BridgeModule.messageFlow)
            message.edit { content = "Saving token." }
            properties.setProperty("mc", mcTokenData.access_token)
            properties.store(FileOutputStream("/opt/kamo/config"), null)
            message.edit { content = "All done!" }
        }
    }
}