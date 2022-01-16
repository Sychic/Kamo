package kamo.modules

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.cache.api.count
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.cache.data.GuildData
import dev.kord.core.cache.data.UserData
import dev.kord.core.event.gateway.ReadyEvent
import kotlinx.coroutines.delay

object PresenceModule : Extension() {
    override val name: String = "presence"

    override suspend fun setup() {
        event<ReadyEvent> {
            action {
                delay(100L)
                val users = kord.cache.count<UserData>()
                val guilds = kord.cache.count<GuildData>()
                kord.editPresence {
                    status = PresenceStatus.Online
                    watching("$users users and $guilds guilds")
                }

            }
        }
    }
}