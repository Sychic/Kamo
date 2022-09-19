package kamo.modules.impl

import dev.kord.common.Color
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.createRole
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.on
import kamo.modules.Module
import kotlinx.coroutines.flow.firstOrNull

object JoinModule: Module() {
    override val name = "join"

    override suspend fun setup() {
        kord.on<MemberJoinEvent> {
            if (this.member.isBot == true) return@on
            member.addRole(guild.findOrCreateRole("Unverified").id, "Auto unverified")
        }
    }

    private suspend fun GuildBehavior.findOrCreateRole(roleName: String) =
        roles.firstOrNull { it.name == roleName } ?: createRole {
            name = roleName
            color = Color(0x000000)
            reason = "create $roleName role"
        }
}