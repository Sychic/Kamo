package kamo.modules

import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.common.Color
import dev.kord.core.behavior.createRole
import dev.kord.core.event.guild.MemberJoinEvent
import kotlinx.coroutines.flow.firstOrNull

object JoinModule: Extension() {
    override val name = "join"

    override suspend fun setup() {
        event<MemberJoinEvent> {
            check {
                isNotBot()
            }

            action {
                event.guild.roles.firstOrNull { it.name == "Unverified" }?.let { role ->
                    event.member.addRole(role.id, "Auto unverified")
                } ?: event.guild.createRole {
                    name = "Unverified"
                    color = Color(0x000000)
                    reason = "Create unverified role"
                }.let { role ->
                    event.member.addRole(role.id, "Auto unverified")
                }
                event.guild.roles.firstOrNull { it.name == "Ducks" }?.let { role ->
                    event.member.addRole(role.id, "Auto ducks")
                } ?: event.guild.createRole {
                    name = "Ducks"
                    color = Color(0x000000)
                    reason = "Create unverified role"
                }.let { role ->
                    event.member.addRole(role.id, "Auto ducks")
                }
            }
        }
    }
}